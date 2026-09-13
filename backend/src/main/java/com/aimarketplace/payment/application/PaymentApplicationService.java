package com.aimarketplace.payment.application;

import com.aimarketplace.order.domain.model.OrderStatus;
import com.aimarketplace.order.dto.OrderResponse;
import com.aimarketplace.order.entity.Order;
import com.aimarketplace.order.repository.OrderRepository;
import com.aimarketplace.payment.domain.model.*;
import com.aimarketplace.payment.dto.CreatePaymentSessionRequest;
import com.aimarketplace.payment.dto.PaymentSessionResponse;
import com.aimarketplace.payment.dto.ProviderMethodsResponse;
import com.aimarketplace.payment.entity.LedgerEntry;
import com.aimarketplace.payment.entity.Payment;
import com.aimarketplace.payment.entity.PaymentWebhookEvent;
import com.aimarketplace.payment.event.OrderPaidEscrowEvent;
import com.aimarketplace.payment.event.PaymentCapturedEvent;
import com.aimarketplace.payment.gateway.*;
import com.aimarketplace.payment.repository.LedgerEntryRepository;
import com.aimarketplace.payment.repository.PaymentRepository;
import com.aimarketplace.payment.repository.PaymentWebhookEventRepository;
import com.aimarketplace.shared.config.PaymentProperties;
import com.aimarketplace.shared.exception.ApiException;
import com.aimarketplace.shared.security.SecurityUtils;
import com.aimarketplace.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor
@Service
public class PaymentApplicationService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final PaymentProperties paymentProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final com.aimarketplace.order.application.OrderApplicationService orderApplicationService;

    @Transactional(readOnly = true)
    public List<ProviderMethodsResponse> listProviderMethods() {
        List<ProviderMethodsResponse> result = new ArrayList<>();
        for (PaymentGateway gateway : gatewayRegistry.all()) {
            String notes = switch (gateway.provider()) {
                case RAZORPAY -> "India-first: cards, UPI (GPay/PhonePe/Paytm UPI), wallets, netbanking via Razorpay Checkout";
                case STRIPE -> "Cards plus Apple Pay / Google Pay / Link via Stripe Payment Element (domain verification required for wallets)";
                case SIMULATED -> "Local demo provider — no real money moves; use Confirm simulated payment";
            };
            result.add(new ProviderMethodsResponse(
                    gateway.provider(),
                    gateway.enabled(),
                    gateway.supportedMethods(),
                    gateway.supportedCurrencies(),
                    notes
            ));
        }
        result.sort(Comparator.comparing(r -> r.provider().name()));
        return result;
    }

    @Transactional
    public PaymentSessionResponse createSession(Long orderId, CreatePaymentSessionRequest request) {
        UserPrincipal user = SecurityUtils.currentUser();
        Order order = fetchOrder(orderId);
        assertClient(order, user.id());
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new ApiException(HttpStatus.CONFLICT, "Only orders awaiting payment can be paid");
        }
        if (paymentRepository.existsByOrderIdAndTypeAndStatus(orderId, PaymentType.CAPTURE, PaymentStatus.SUCCESS)) {
            throw new ApiException(HttpStatus.CONFLICT, "This order is already paid");
        }

        PaymentProvider provider = request == null || request.provider() == null
                ? defaultProvider(order.getCurrency())
                : request.provider();
        PaymentMethodHint method = request == null || request.preferredMethod() == null
                ? PaymentMethodHint.ANY
                : request.preferredMethod();

        PaymentGateway gateway = gatewayRegistry.require(provider);
        if (!gateway.supportedCurrencies().contains(order.getCurrency().toUpperCase())) {
            throw ApiException.ofField(HttpStatus.BAD_REQUEST, "Please fix the highlighted fields",
                    "provider", provider + " does not support currency " + order.getCurrency());
        }
        if (!gateway.supportedMethods().contains(method) && method != PaymentMethodHint.ANY) {
            throw ApiException.ofField(HttpStatus.BAD_REQUEST, "Please fix the highlighted fields",
                    "preferredMethod", method + " is not supported by " + provider);
        }

        String idempotencyKey = "CAPTURE:" + orderId + ":" + provider;
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.PENDING
                && existing.get().getProviderOrderId() != null) {
            return toSessionResponse(existing.get(), order, gateway);
        }

        String email = user.email();
        PaymentSessionResult session = gateway.createCaptureSession(new CreatePaymentSessionCommand(
                order.getId(),
                order.getOrderNumber(),
                order.getAmount(),
                order.getCurrency(),
                idempotencyKey,
                method,
                email,
                "AI Marketplace order " + order.getOrderNumber()
        ));

        Payment payment = existing.orElseGet(Payment::new);
        payment.setOrderId(order.getId());
        payment.setProvider(provider);
        payment.setPreferredMethod(method);
        payment.setProviderOrderId(session.providerOrderId());
        payment.setProviderPaymentId(session.providerPaymentId());
        payment.setType(PaymentType.CAPTURE);
        payment.setAmount(order.getAmount());
        payment.setCurrency(order.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setClientSecret(session.clientSecret());
        payment.setRawResponseJson(session.rawJson());
        Payment saved = paymentRepository.save(payment);
        return toSessionResponse(saved, order, gateway, session);
    }

    @Transactional
    public OrderResponse simulateCapture(Long orderId, PaymentProvider provider) {
        if (!paymentProperties.allowSimulate()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Simulated payments are disabled");
        }
        UserPrincipal user = SecurityUtils.currentUser();
        Order order = fetchOrder(orderId);
        assertClient(order, user.id());
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new ApiException(HttpStatus.CONFLICT, "Only orders awaiting payment can be paid");
        }

        PaymentProvider resolved = provider == null ? PaymentProvider.SIMULATED : provider;
        PaymentSessionResponse session = createSession(orderId, new CreatePaymentSessionRequest(resolved, PaymentMethodHint.ANY));
        markCaptureSuccess(
                session.paymentId(),
                session.providerOrderId(),
                session.providerPaymentId() == null ? "sim_confirmed" : session.providerPaymentId(),
                "{\"source\":\"simulate\"}"
        );
        return orderApplicationService.getOrder(orderId);
    }

    @Transactional
    public void handleWebhook(PaymentProvider provider, String payload, Map<String, String> headers) {
        PaymentGateway gateway = gatewayRegistry.require(provider);
        WebhookParseResult parsed = gateway.parseWebhook(payload, headers);

        if (webhookEventRepository.existsByProviderAndEventId(provider, parsed.eventId())) {
            return;
        }

        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setProvider(provider);
        event.setEventId(parsed.eventId());
        event.setEventType(parsed.eventType());
        event.setPayloadJson(payload);
        try {
            webhookEventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException ex) {
            return;
        }

        if (parsed.ignored() || !parsed.success()) {
            event.setProcessed(true);
            event.setProcessedAt(Instant.now());
            webhookEventRepository.save(event);
            return;
        }

        Payment payment = resolvePayment(provider, parsed);
        markCaptureSuccess(payment.getId(), parsed.providerOrderId(), parsed.providerPaymentId(), payload);

        event.setProcessed(true);
        event.setProcessedAt(Instant.now());
        webhookEventRepository.save(event);
    }

    private Payment resolvePayment(PaymentProvider provider, WebhookParseResult parsed) {
        if (parsed.providerOrderId() != null) {
            Optional<Payment> byOrder = paymentRepository.findByProviderAndProviderOrderId(provider, parsed.providerOrderId());
            if (byOrder.isPresent()) {
                return byOrder.get();
            }
        }
        if (parsed.providerPaymentId() != null) {
            Optional<Payment> byPayment = paymentRepository.findByProviderAndProviderPaymentId(provider, parsed.providerPaymentId());
            if (byPayment.isPresent()) {
                return byPayment.get();
            }
        }
        if (parsed.orderNumberHint() != null) {
            Order order = orderRepository.findByOrderNumber(parsed.orderNumberHint())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order for webhook not found"));
            return paymentRepository.findByOrderIdAndTypeOrderByCreatedAtDesc(order.getId(), PaymentType.CAPTURE).stream()
                    .filter(p -> p.getProvider() == provider)
                    .findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment for webhook not found"));
        }
        throw new ApiException(HttpStatus.NOT_FOUND, "Unable to match webhook to a payment");
    }

    private void markCaptureSuccess(Long paymentId, String providerOrderId, String providerPaymentId, String rawJson) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return;
        }

        Order order = fetchOrder(payment.getOrderId());
        if (order.getStatus() == OrderStatus.PAID_ESCROW) {
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);
            return;
        }
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new ApiException(HttpStatus.CONFLICT, "Order is not awaiting payment");
        }

        if (providerOrderId != null) {
            payment.setProviderOrderId(providerOrderId);
        }
        if (providerPaymentId != null) {
            payment.setProviderPaymentId(providerPaymentId);
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setRawResponseJson(rawJson);
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.PAID_ESCROW);
        orderRepository.save(order);

        if (!ledgerEntryRepository.existsByOrderIdAndEntryType(order.getId(), LedgerEntryType.ESCROW_HOLD)) {
            LedgerEntry hold = new LedgerEntry();
            hold.setOrderId(order.getId());
            hold.setPaymentId(payment.getId());
            hold.setEntryType(LedgerEntryType.ESCROW_HOLD);
            hold.setAmount(order.getAmount());
            hold.setCurrency(order.getCurrency());
            hold.setDescription("Client payment held in escrow");
            ledgerEntryRepository.save(hold);

            LedgerEntry fee = new LedgerEntry();
            fee.setOrderId(order.getId());
            fee.setPaymentId(payment.getId());
            fee.setEntryType(LedgerEntryType.PLATFORM_FEE);
            fee.setAmount(order.getPlatformFee());
            fee.setCurrency(order.getCurrency());
            fee.setDescription("Platform fee reserved");
            ledgerEntryRepository.save(fee);
        }

        eventPublisher.publishEvent(new PaymentCapturedEvent(
                payment.getId(), order.getId(), payment.getProvider(), order.getAmount(), order.getCurrency()));
        eventPublisher.publishEvent(new OrderPaidEscrowEvent(
                order.getId(), order.getOrderNumber(), order.getClientId(), order.getFreelancerId(),
                order.getAmount(), order.getCurrency()));
    }

    private PaymentProvider defaultProvider(String currency) {
        if ("INR".equalsIgnoreCase(currency)) {
            PaymentGateway razorpay = gatewayRegistry.all().stream()
                    .filter(g -> g.provider() == PaymentProvider.RAZORPAY && g.enabled())
                    .findFirst().orElse(null);
            if (razorpay != null) {
                return PaymentProvider.RAZORPAY;
            }
        }
        PaymentGateway stripe = gatewayRegistry.all().stream()
                .filter(g -> g.provider() == PaymentProvider.STRIPE && g.enabled())
                .findFirst().orElse(null);
        if (stripe != null) {
            return PaymentProvider.STRIPE;
        }
        if (paymentProperties.allowSimulate()) {
            return PaymentProvider.SIMULATED;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "No payment provider is configured");
    }

    private Order fetchOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private void assertClient(Order order, Long userId) {
        if (!Objects.equals(order.getClientId(), userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the client can pay for this order");
        }
    }

    private PaymentSessionResponse toSessionResponse(Payment payment, Order order, PaymentGateway gateway) {
        Map<String, Object> checkout = new LinkedHashMap<>();
        checkout.put("provider", payment.getProvider().name());
        if (payment.getProvider() == PaymentProvider.RAZORPAY) {
            checkout.put("keyId", paymentProperties.razorpay().keyId());
            checkout.put("orderId", payment.getProviderOrderId());
            checkout.put("amount", payment.getAmount().movePointRight(2).longValue());
            checkout.put("currency", payment.getCurrency());
            checkout.put("name", "AI Marketplace");
            checkout.put("description", "Order " + order.getOrderNumber());
        } else if (payment.getProvider() == PaymentProvider.STRIPE) {
            checkout.put("publishableKey", paymentProperties.stripe().publishableKey());
            checkout.put("sessionId", payment.getProviderOrderId());
            checkout.put("checkoutUrl", null);
        } else {
            checkout.put("simulate", true);
            checkout.put("message", "Confirm simulated payment from the order page");
        }
        return toSessionResponse(payment, order, gateway, new PaymentSessionResult(
                payment.getProviderOrderId(),
                payment.getProviderPaymentId(),
                payment.getClientSecret(),
                payment.getProvider() == PaymentProvider.STRIPE
                        ? paymentProperties.stripe().publishableKey()
                        : payment.getProvider() == PaymentProvider.RAZORPAY
                        ? paymentProperties.razorpay().keyId()
                        : "simulated",
                checkout,
                payment.getRawResponseJson()
        ));
    }

    private PaymentSessionResponse toSessionResponse(
            Payment payment,
            Order order,
            PaymentGateway gateway,
            PaymentSessionResult session
    ) {
        Map<String, Object> checkout = new LinkedHashMap<>();
        if (session.checkoutHints() != null) {
            checkout.putAll(session.checkoutHints());
        }
        checkout.putIfAbsent("provider", payment.getProvider().name());
        checkout.putIfAbsent("orderId", payment.getProviderOrderId());
        checkout.putIfAbsent("clientSecret", payment.getClientSecret());

        return new PaymentSessionResponse(
                payment.getId(),
                order.getId(),
                order.getOrderNumber(),
                payment.getProvider(),
                payment.getPreferredMethod(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProviderOrderId(),
                payment.getProviderPaymentId(),
                payment.getClientSecret(),
                session.publishableKey(),
                checkout
        );
    }
}

package com.aimarketplace.payment.gateway.stripe;

import com.aimarketplace.payment.domain.model.PaymentMethodHint;
import com.aimarketplace.payment.domain.model.PaymentProvider;
import com.aimarketplace.payment.gateway.CreatePaymentSessionCommand;
import com.aimarketplace.payment.gateway.PaymentGateway;
import com.aimarketplace.payment.gateway.PaymentSessionResult;
import com.aimarketplace.payment.gateway.WebhookParseResult;
import com.aimarketplace.shared.config.PaymentProperties;
import com.aimarketplace.shared.exception.ApiException;
import com.google.gson.JsonSyntaxException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class StripePaymentGateway implements PaymentGateway {
    private final PaymentProperties properties;

    public StripePaymentGateway(PaymentProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.STRIPE;
    }

    @Override
    public boolean enabled() {
        return properties.stripe() != null && properties.stripe().configured();
    }

    @Override
    public List<PaymentMethodHint> supportedMethods() {
        return List.of(
                PaymentMethodHint.CARD,
                PaymentMethodHint.APPLE_PAY,
                PaymentMethodHint.GOOGLE_PAY,
                PaymentMethodHint.LINK,
                PaymentMethodHint.ANY
        );
    }

    @Override
    public List<String> supportedCurrencies() {
        return List.of("INR", "USD", "EUR", "GBP");
    }

    @Override
    public PaymentSessionResult createCaptureSession(CreatePaymentSessionCommand command) {
        assertEnabled();
        Stripe.apiKey = properties.stripe().secretKey();
        try {
            String successUrl = properties.successUrl()
                    + (properties.successUrl().contains("?") ? "&" : "?")
                    + "orderId=" + command.orderId()
                    + "&session_id={CHECKOUT_SESSION_ID}";
            String cancelUrl = properties.cancelUrl()
                    + (properties.cancelUrl().contains("?") ? "&" : "?")
                    + "orderId=" + command.orderId();

            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .setClientReferenceId(command.orderNumber())
                    .putMetadata("marketplaceOrderId", String.valueOf(command.orderId()))
                    .putMetadata("orderNumber", command.orderNumber())
                    .putMetadata("idempotencyKey", command.idempotencyKey())
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(command.currency().toLowerCase())
                                                    .setUnitAmount(toMinorUnits(command.amount()))
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("AI Marketplace " + command.orderNumber())
                                                                    .setDescription(command.description())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    );

            if (command.customerEmail() != null && !command.customerEmail().isBlank()) {
                builder.setCustomerEmail(command.customerEmail());
            }
            if (command.preferredMethod() != null && command.preferredMethod() != PaymentMethodHint.ANY) {
                builder.putMetadata("preferredMethod", command.preferredMethod().name());
            }

            Session session = Session.create(
                    builder.build(),
                    com.stripe.net.RequestOptions.builder()
                            .setIdempotencyKey(command.idempotencyKey())
                            .build()
            );

            Map<String, Object> hints = new LinkedHashMap<>();
            hints.put("publishableKey", properties.stripe().publishableKey());
            hints.put("checkoutUrl", session.getUrl());
            hints.put("sessionId", session.getId());
            hints.put("wallets", List.of("apple_pay", "google_pay", "link"));
            hints.put("preferredMethod", command.preferredMethod() == null
                    ? PaymentMethodHint.ANY.name()
                    : command.preferredMethod().name());

            return new PaymentSessionResult(
                    session.getId(),
                    session.getPaymentIntent(),
                    null,
                    properties.stripe().publishableKey(),
                    hints,
                    session.toJson()
            );
        } catch (StripeException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Stripe session failed: " + ex.getMessage());
        }
    }

    @Override
    public WebhookParseResult parseWebhook(String payload, Map<String, String> headers) {
        assertEnabled();
        String signature = headers.getOrDefault("stripe-signature", headers.get("Stripe-Signature"));
        String secret = properties.stripe().webhookSecret();
        Event event;
        try {
            if (secret != null && !secret.isBlank()) {
                if (signature == null || signature.isBlank()) {
                    throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing Stripe webhook signature");
                }
                event = Webhook.constructEvent(payload, signature, secret);
            } else {
                event = Event.GSON.fromJson(payload, Event.class);
            }
        } catch (SignatureVerificationException | JsonSyntaxException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid Stripe webhook signature or payload");
        }

        String eventId = event.getId();
        String eventType = event.getType();
        if (!"checkout.session.completed".equals(eventType) && !"payment_intent.succeeded".equals(eventType)) {
            return WebhookParseResult.ignored(eventId, eventType);
        }

        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeObject == null) {
            try {
                stripeObject = event.getDataObjectDeserializer().deserializeUnsafe();
            } catch (Exception ignored) {
                return WebhookParseResult.ignored(eventId, eventType);
            }
        }

        if (stripeObject instanceof Session session) {
            if (!"paid".equalsIgnoreCase(session.getPaymentStatus()) && session.getPaymentStatus() != null) {
                return WebhookParseResult.ignored(eventId, eventType);
            }
            String orderNumber = session.getMetadata() == null ? session.getClientReferenceId()
                    : session.getMetadata().getOrDefault("orderNumber", session.getClientReferenceId());
            return new WebhookParseResult(
                    eventId,
                    eventType,
                    true,
                    false,
                    session.getId(),
                    session.getPaymentIntent(),
                    orderNumber
            );
        }

        if (stripeObject instanceof com.stripe.model.PaymentIntent paymentIntent) {
            String orderNumber = paymentIntent.getMetadata() == null
                    ? null
                    : paymentIntent.getMetadata().get("orderNumber");
            return new WebhookParseResult(
                    eventId,
                    eventType,
                    true,
                    false,
                    paymentIntent.getId(),
                    paymentIntent.getId(),
                    orderNumber
            );
        }

        return WebhookParseResult.ignored(eventId, eventType);
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private void assertEnabled() {
        if (!enabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Stripe is not configured");
        }
    }
}

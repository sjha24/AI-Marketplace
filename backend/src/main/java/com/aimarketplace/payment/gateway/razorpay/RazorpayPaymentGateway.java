package com.aimarketplace.payment.gateway.razorpay;

import com.aimarketplace.payment.domain.model.PaymentMethodHint;
import com.aimarketplace.payment.domain.model.PaymentProvider;
import com.aimarketplace.payment.gateway.CreatePaymentSessionCommand;
import com.aimarketplace.payment.gateway.PaymentGateway;
import com.aimarketplace.payment.gateway.PaymentSessionResult;
import com.aimarketplace.payment.gateway.WebhookParseResult;
import com.aimarketplace.shared.config.PaymentProperties;
import com.aimarketplace.shared.exception.ApiException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RazorpayPaymentGateway implements PaymentGateway {
    private final PaymentProperties properties;

    public RazorpayPaymentGateway(PaymentProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.RAZORPAY;
    }

    @Override
    public boolean enabled() {
        return properties.razorpay() != null && properties.razorpay().configured();
    }

    @Override
    public List<PaymentMethodHint> supportedMethods() {
        return List.of(
                PaymentMethodHint.CARD,
                PaymentMethodHint.UPI,
                PaymentMethodHint.GPAY,
                PaymentMethodHint.PHONEPE,
                PaymentMethodHint.PAYTM,
                PaymentMethodHint.NETBANKING,
                PaymentMethodHint.WALLET,
                PaymentMethodHint.ANY
        );
    }

    @Override
    public List<String> supportedCurrencies() {
        return List.of("INR");
    }

    @Override
    public PaymentSessionResult createCaptureSession(CreatePaymentSessionCommand command) {
        assertEnabled();
        if (!"INR".equalsIgnoreCase(command.currency())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Razorpay currently supports INR only in this project");
        }
        try {
            RazorpayClient client = new RazorpayClient(properties.razorpay().keyId(), properties.razorpay().keySecret());
            JSONObject request = new JSONObject();
            request.put("amount", toPaise(command.amount()));
            request.put("currency", command.currency().toUpperCase());
            request.put("receipt", truncate(command.orderNumber(), 40));
            request.put("payment_capture", 1);
            JSONObject notes = new JSONObject();
            notes.put("marketplaceOrderId", String.valueOf(command.orderId()));
            notes.put("orderNumber", command.orderNumber());
            notes.put("idempotencyKey", command.idempotencyKey());
            if (command.preferredMethod() != null) {
                notes.put("preferredMethod", command.preferredMethod().name());
            }
            request.put("notes", notes);

            Order order = client.orders.create(request);
            String providerOrderId = order.get("id");

            Map<String, Object> hints = new LinkedHashMap<>();
            hints.put("keyId", properties.razorpay().keyId());
            hints.put("name", "AI Marketplace");
            hints.put("description", command.description());
            hints.put("currency", command.currency().toUpperCase());
            hints.put("amount", toPaise(command.amount()));
            hints.put("prefill", Map.of("email", command.customerEmail() == null ? "" : command.customerEmail()));
            hints.put("method", razorpayMethodConfig(command.preferredMethod()));
            hints.put("theme", Map.of("color", "#335cff"));

            return new PaymentSessionResult(
                    providerOrderId,
                    null,
                    null,
                    properties.razorpay().keyId(),
                    hints,
                    order.toString()
            );
        } catch (RazorpayException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Razorpay session failed: " + ex.getMessage());
        }
    }

    @Override
    public WebhookParseResult parseWebhook(String payload, Map<String, String> headers) {
        assertEnabled();
        String signature = headers.getOrDefault("x-razorpay-signature", headers.get("X-Razorpay-Signature"));
        String secret = properties.razorpay().webhookSecret();
        if (secret != null && !secret.isBlank()) {
            if (signature == null || signature.isBlank()) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing Razorpay webhook signature");
            }
            try {
                Utils.verifyWebhookSignature(payload, signature, secret);
            } catch (RazorpayException ex) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid Razorpay webhook signature");
            }
        }

        JSONObject json = new JSONObject(payload);
        String eventType = json.optString("event", "unknown");
        String eventId = json.optString("id", null);
        if (eventId == null || eventId.isBlank()) {
            eventId = "rzp-" + Integer.toHexString(payload.hashCode()) + "-" + eventType;
        }

        if (!"payment.captured".equals(eventType) && !"order.paid".equals(eventType)) {
            return WebhookParseResult.ignored(eventId, eventType);
        }

        JSONObject payloadObj = json.optJSONObject("payload");
        if (payloadObj == null) {
            return WebhookParseResult.ignored(eventId, eventType);
        }

        String providerPaymentId = null;
        String providerOrderId = null;
        String orderNumber = null;

        JSONObject paymentEntity = nestedEntity(payloadObj, "payment");
        if (paymentEntity != null) {
            providerPaymentId = paymentEntity.optString("id", null);
            providerOrderId = paymentEntity.optString("order_id", null);
            JSONObject notes = paymentEntity.optJSONObject("notes");
            if (notes != null) {
                orderNumber = notes.optString("orderNumber", null);
            }
            String status = paymentEntity.optString("status", "");
            if (!"captured".equalsIgnoreCase(status) && !"payment.captured".equals(eventType)) {
                return WebhookParseResult.ignored(eventId, eventType);
            }
        }

        JSONObject orderEntity = nestedEntity(payloadObj, "order");
        if (orderEntity != null) {
            if (providerOrderId == null) {
                providerOrderId = orderEntity.optString("id", null);
            }
            JSONObject notes = orderEntity.optJSONObject("notes");
            if (notes != null && (orderNumber == null || orderNumber.isBlank())) {
                orderNumber = notes.optString("orderNumber", null);
            }
        }

        return new WebhookParseResult(eventId, eventType, true, false, providerOrderId, providerPaymentId, orderNumber);
    }

    private JSONObject nestedEntity(JSONObject payloadObj, String key) {
        JSONObject wrapper = payloadObj.optJSONObject(key);
        if (wrapper == null) {
            return null;
        }
        return wrapper.optJSONObject("entity");
    }

    private Map<String, Object> razorpayMethodConfig(PaymentMethodHint hint) {
        if (hint == null || hint == PaymentMethodHint.ANY) {
            return Map.of(
                    "card", true,
                    "upi", true,
                    "netbanking", true,
                    "wallet", true
            );
        }
        return switch (hint) {
            case CARD -> Map.of("card", true, "upi", false, "netbanking", false, "wallet", false);
            case UPI, GPAY, PHONEPE -> Map.of("card", false, "upi", true, "netbanking", false, "wallet", false);
            case PAYTM, WALLET -> Map.of("card", false, "upi", true, "netbanking", false, "wallet", true);
            case NETBANKING -> Map.of("card", false, "upi", false, "netbanking", true, "wallet", false);
            default -> Map.of("card", true, "upi", true, "netbanking", true, "wallet", true);
        };
    }

    private long toPaise(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private void assertEnabled() {
        if (!enabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Razorpay is not configured");
        }
    }
}

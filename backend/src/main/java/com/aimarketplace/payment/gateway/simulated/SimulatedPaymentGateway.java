package com.aimarketplace.payment.gateway.simulated;

import com.aimarketplace.payment.domain.model.PaymentMethodHint;
import com.aimarketplace.payment.domain.model.PaymentProvider;
import com.aimarketplace.payment.gateway.CreatePaymentSessionCommand;
import com.aimarketplace.payment.gateway.PaymentGateway;
import com.aimarketplace.payment.gateway.PaymentSessionResult;
import com.aimarketplace.payment.gateway.WebhookParseResult;
import com.aimarketplace.shared.config.PaymentProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SimulatedPaymentGateway implements PaymentGateway {
    private final PaymentProperties properties;

    public SimulatedPaymentGateway(PaymentProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.SIMULATED;
    }

    @Override
    public boolean enabled() {
        return properties.allowSimulate();
    }

    @Override
    public List<PaymentMethodHint> supportedMethods() {
        return List.of(
                PaymentMethodHint.CARD,
                PaymentMethodHint.UPI,
                PaymentMethodHint.GPAY,
                PaymentMethodHint.PHONEPE,
                PaymentMethodHint.PAYTM,
                PaymentMethodHint.APPLE_PAY,
                PaymentMethodHint.GOOGLE_PAY,
                PaymentMethodHint.ANY
        );
    }

    @Override
    public List<String> supportedCurrencies() {
        return List.of("INR", "USD", "EUR", "GBP");
    }

    @Override
    public PaymentSessionResult createCaptureSession(CreatePaymentSessionCommand command) {
        String sessionId = "sim_order_" + command.orderId() + "_" + UUID.randomUUID().toString().substring(0, 8);
        String paymentId = "sim_pay_" + UUID.randomUUID().toString().substring(0, 12);
        Map<String, Object> hints = new LinkedHashMap<>();
        hints.put("simulate", true);
        hints.put("message", "Local simulated checkout — confirm from the order page");
        hints.put("preferredMethod", command.preferredMethod() == null ? PaymentMethodHint.ANY.name() : command.preferredMethod().name());
        return new PaymentSessionResult(sessionId, paymentId, null, "simulated", hints,
                "{\"provider\":\"SIMULATED\",\"sessionId\":\"" + sessionId + "\"}");
    }

    @Override
    public WebhookParseResult parseWebhook(String payload, Map<String, String> headers) {
        return WebhookParseResult.ignored("sim-ignored", "simulated.webhook");
    }
}

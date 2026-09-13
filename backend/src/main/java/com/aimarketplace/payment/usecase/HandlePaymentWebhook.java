package com.aimarketplace.payment.usecase;

import com.aimarketplace.payment.application.PaymentApplicationService;
import com.aimarketplace.payment.domain.model.PaymentProvider;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class HandlePaymentWebhook {
    private final PaymentApplicationService service;

    @Logging
    public void execute(String providerPath, String payload, Map<String, String> headers) {
        ValidationUtil validation = ValidationUtil.create();
        validation.require(providerPath != null && !providerPath.isBlank(), "provider", "Provider is required");
        validation.require(payload != null && !payload.isBlank(), "payload", "Webhook payload is required");
        validation.validate();

        PaymentProvider provider = switch (providerPath.trim().toLowerCase(Locale.ROOT)) {
            case "razorpay" -> PaymentProvider.RAZORPAY;
            case "stripe" -> PaymentProvider.STRIPE;
            default -> throw new com.aimarketplace.shared.exception.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Unknown payment provider webhook");
        };
        service.handleWebhook(provider, payload, headers);
    }
}

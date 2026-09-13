package com.aimarketplace.payment.gateway;

import java.util.Map;

public record PaymentSessionResult(
        String providerOrderId,
        String providerPaymentId,
        String clientSecret,
        String publishableKey,
        Map<String, Object> checkoutHints,
        String rawJson
) {
}

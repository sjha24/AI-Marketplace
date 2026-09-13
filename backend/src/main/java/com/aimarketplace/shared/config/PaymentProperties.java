package com.aimarketplace.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payments")
public record PaymentProperties(
        boolean allowSimulate,
        String successUrl,
        String cancelUrl,
        Razorpay razorpay,
        Stripe stripe
) {
    public record Razorpay(String keyId, String keySecret, String webhookSecret) {
        public boolean configured() {
            return notBlank(keyId) && notBlank(keySecret);
        }
    }

    public record Stripe(String secretKey, String publishableKey, String webhookSecret) {
        public boolean configured() {
            return notBlank(secretKey) && notBlank(publishableKey);
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}

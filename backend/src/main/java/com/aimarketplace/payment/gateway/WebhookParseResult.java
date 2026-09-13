package com.aimarketplace.payment.gateway;

public record WebhookParseResult(
        String eventId,
        String eventType,
        boolean success,
        boolean ignored,
        String providerOrderId,
        String providerPaymentId,
        String orderNumberHint
) {
    public static WebhookParseResult ignored(String eventId, String eventType) {
        return new WebhookParseResult(eventId, eventType, false, true, null, null, null);
    }
}

package com.aimarketplace.payment.gateway;

import com.aimarketplace.payment.domain.model.PaymentMethodHint;

import java.math.BigDecimal;

public record CreatePaymentSessionCommand(
        Long orderId,
        String orderNumber,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        PaymentMethodHint preferredMethod,
        String customerEmail,
        String description
) {
}

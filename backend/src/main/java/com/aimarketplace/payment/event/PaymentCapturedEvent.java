package com.aimarketplace.payment.event;

import com.aimarketplace.payment.domain.model.PaymentProvider;

import java.math.BigDecimal;

public record PaymentCapturedEvent(
        Long paymentId,
        Long orderId,
        PaymentProvider provider,
        BigDecimal amount,
        String currency
) {
}

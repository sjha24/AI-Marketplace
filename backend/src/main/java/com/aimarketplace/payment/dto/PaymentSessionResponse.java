package com.aimarketplace.payment.dto;

import com.aimarketplace.payment.domain.model.PaymentMethodHint;
import com.aimarketplace.payment.domain.model.PaymentProvider;
import com.aimarketplace.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.Map;

public record PaymentSessionResponse(
        Long paymentId,
        Long orderId,
        String orderNumber,
        PaymentProvider provider,
        PaymentMethodHint preferredMethod,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String providerOrderId,
        String providerPaymentId,
        String clientSecret,
        String publishableKey,
        Map<String, Object> checkout
) {
}

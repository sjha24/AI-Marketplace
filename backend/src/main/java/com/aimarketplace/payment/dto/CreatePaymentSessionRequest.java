package com.aimarketplace.payment.dto;

import com.aimarketplace.payment.domain.model.PaymentMethodHint;
import com.aimarketplace.payment.domain.model.PaymentProvider;

public record CreatePaymentSessionRequest(
        PaymentProvider provider,
        PaymentMethodHint preferredMethod
) {
}

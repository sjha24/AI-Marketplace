package com.aimarketplace.payment.dto;

import com.aimarketplace.payment.domain.model.PaymentMethodHint;
import com.aimarketplace.payment.domain.model.PaymentProvider;

import java.util.List;

public record ProviderMethodsResponse(
        PaymentProvider provider,
        boolean enabled,
        List<PaymentMethodHint> methods,
        List<String> currencies,
        String notes
) {
}

package com.aimarketplace.payment.usecase;

import com.aimarketplace.payment.application.PaymentApplicationService;
import com.aimarketplace.payment.dto.ProviderMethodsResponse;
import com.aimarketplace.shared.aop.Logging;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListPaymentMethods {
    private final PaymentApplicationService service;

    @Logging
    public List<ProviderMethodsResponse> execute() {
        return service.listProviderMethods();
    }
}

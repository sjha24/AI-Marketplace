package com.aimarketplace.payment.usecase;

import com.aimarketplace.order.dto.OrderResponse;
import com.aimarketplace.payment.application.PaymentApplicationService;
import com.aimarketplace.payment.domain.model.PaymentProvider;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SimulatePaymentCapture {
    private final PaymentApplicationService service;

    @Logging
    public OrderResponse execute(Long orderId, PaymentProvider provider) {
        ValidationUtil.create()
                .require(orderId != null && orderId > 0, "orderId", "Order id is required")
                .validate();
        return service.simulateCapture(orderId, provider == null ? PaymentProvider.SIMULATED : provider);
    }
}

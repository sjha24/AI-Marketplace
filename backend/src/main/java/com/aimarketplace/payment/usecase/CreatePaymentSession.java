package com.aimarketplace.payment.usecase;

import com.aimarketplace.payment.application.PaymentApplicationService;
import com.aimarketplace.payment.domain.model.PaymentMethodHint;
import com.aimarketplace.payment.dto.CreatePaymentSessionRequest;
import com.aimarketplace.payment.dto.PaymentSessionResponse;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreatePaymentSession {
    private final PaymentApplicationService service;

    @Logging
    public PaymentSessionResponse execute(Long orderId, CreatePaymentSessionRequest request) {
        ValidationUtil.create()
                .require(orderId != null && orderId > 0, "orderId", "Order id is required")
                .validate();

        CreatePaymentSessionRequest normalized = request == null
                ? new CreatePaymentSessionRequest(null, PaymentMethodHint.ANY)
                : new CreatePaymentSessionRequest(
                request.provider(),
                request.preferredMethod() == null ? PaymentMethodHint.ANY : request.preferredMethod()
        );
        return service.createSession(orderId, normalized);
    }
}

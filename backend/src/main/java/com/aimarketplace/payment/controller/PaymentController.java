package com.aimarketplace.payment.controller;

import com.aimarketplace.order.dto.OrderResponse;
import com.aimarketplace.payment.domain.model.PaymentProvider;
import com.aimarketplace.payment.dto.CreatePaymentSessionRequest;
import com.aimarketplace.payment.dto.PaymentSessionResponse;
import com.aimarketplace.payment.dto.ProviderMethodsResponse;
import com.aimarketplace.payment.usecase.CreatePaymentSession;
import com.aimarketplace.payment.usecase.ListPaymentMethods;
import com.aimarketplace.payment.usecase.SimulatePaymentCapture;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final ListPaymentMethods listPaymentMethods;
    private final CreatePaymentSession createPaymentSession;
    private final SimulatePaymentCapture simulatePaymentCapture;

    @GetMapping("/api/payments/methods")
    public List<ProviderMethodsResponse> methods() {
        return listPaymentMethods.execute();
    }

    @PostMapping("/api/orders/{orderId}/pay")
    @PreAuthorize("hasRole('CLIENT')")
    public PaymentSessionResponse pay(
            @PathVariable Long orderId,
            @RequestBody(required = false) CreatePaymentSessionRequest request
    ) {
        return createPaymentSession.execute(orderId, request);
    }

    @PostMapping("/api/orders/{orderId}/pay/simulate")
    @PreAuthorize("hasRole('CLIENT')")
    public OrderResponse simulate(
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        PaymentProvider provider = null;
        if (body != null && body.get("provider") != null) {
            provider = PaymentProvider.valueOf(body.get("provider"));
        }
        return simulatePaymentCapture.execute(orderId, provider);
    }
}

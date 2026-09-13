package com.aimarketplace.payment.gateway;

import com.aimarketplace.payment.domain.model.PaymentMethodHint;
import com.aimarketplace.payment.domain.model.PaymentProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PaymentGateway {
    PaymentProvider provider();

    boolean enabled();

    List<PaymentMethodHint> supportedMethods();

    List<String> supportedCurrencies();

    PaymentSessionResult createCaptureSession(CreatePaymentSessionCommand command);

    WebhookParseResult parseWebhook(String payload, Map<String, String> headers);
}

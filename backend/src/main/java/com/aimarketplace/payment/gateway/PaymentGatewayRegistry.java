package com.aimarketplace.payment.gateway;

import com.aimarketplace.payment.domain.model.PaymentProvider;
import com.aimarketplace.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentGatewayRegistry {
    private final Map<PaymentProvider, PaymentGateway> gateways = new EnumMap<>(PaymentProvider.class);

    public PaymentGatewayRegistry(List<PaymentGateway> gatewayList) {
        for (PaymentGateway gateway : gatewayList) {
            gateways.put(gateway.provider(), gateway);
        }
    }

    public PaymentGateway require(PaymentProvider provider) {
        PaymentGateway gateway = gateways.get(provider);
        if (gateway == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported payment provider: " + provider);
        }
        if (!gateway.enabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    provider + " is not configured. Set API keys or use SIMULATED while allow-simulate is true.");
        }
        return gateway;
    }

    public List<PaymentGateway> all() {
        return List.copyOf(gateways.values());
    }
}

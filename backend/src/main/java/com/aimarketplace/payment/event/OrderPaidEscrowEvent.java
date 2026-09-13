package com.aimarketplace.payment.event;

import java.math.BigDecimal;

public record OrderPaidEscrowEvent(
        Long orderId,
        String orderNumber,
        Long clientId,
        Long freelancerId,
        BigDecimal amount,
        String currency
) {
}

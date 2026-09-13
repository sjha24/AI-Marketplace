package com.aimarketplace.order.event;

import java.math.BigDecimal;

public record OrderCreatedEvent(
        Long orderId,
        String orderNumber,
        Long jobId,
        Long proposalId,
        Long clientId,
        Long freelancerId,
        BigDecimal amount,
        String currency
) {
}

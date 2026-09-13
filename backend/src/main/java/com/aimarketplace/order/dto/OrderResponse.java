package com.aimarketplace.order.dto;

import com.aimarketplace.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long jobId,
        String jobTitle,
        Long proposalId,
        Long clientId,
        String clientDisplayName,
        Long freelancerId,
        String freelancerDisplayName,
        BigDecimal amount,
        BigDecimal platformFee,
        BigDecimal freelancerPayout,
        String currency,
        OrderStatus status,
        String deliveryNote,
        Instant deliveredAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

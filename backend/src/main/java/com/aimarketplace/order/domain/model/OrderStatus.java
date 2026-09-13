package com.aimarketplace.order.domain.model;

public enum OrderStatus {
    CREATED,
    AWAITING_PAYMENT,
    PAID_ESCROW,
    IN_PROGRESS,
    DELIVERED,
    COMPLETED,
    DISPUTED,
    REFUNDED,
    CANCELLED
}

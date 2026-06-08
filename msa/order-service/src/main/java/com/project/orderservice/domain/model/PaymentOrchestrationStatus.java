package com.project.orderservice.domain.model;

public enum PaymentOrchestrationStatus {
    STARTED,
    INVENTORY_DEDUCTED,
    ORDER_COMPLETED,
    COMPENSATING,
    INVENTORY_RESTORED,
    PAYMENT_REFUNDED,
    ORDER_CANCELLED,
    FAILED
}

package com.project.paymentservice.infrastructure.tosspayments.dto;

public record TossPaymentConfirmRequest(
        String paymentKey,
        String orderId,
        Integer amount
) {
}

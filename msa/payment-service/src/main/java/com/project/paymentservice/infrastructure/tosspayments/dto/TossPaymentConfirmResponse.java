package com.project.paymentservice.infrastructure.tosspayments.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentConfirmResponse(
        String paymentKey,
        String orderId,
        String status,
        Integer totalAmount,
        String method,
        OffsetDateTime approvedAt
) {
}

package com.project.paymentservice.infrastructure.tosspayments.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentErrorResponse(
        String code,
        String message
) {
}

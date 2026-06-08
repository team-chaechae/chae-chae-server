package com.project.paymentservice.infrastructure.tosspayments.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentCancelResponse(
        String paymentKey,
        String orderId,
        String status,
        Integer totalAmount,
        String method,
        OffsetDateTime approvedAt,
        List<CancelDetail> cancels
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CancelDetail(
            Integer cancelAmount,
            String cancelReason,
            Integer refundableAmount,
            OffsetDateTime canceledAt,
            String transactionKey,
            String cancelStatus
    ) {
    }
}

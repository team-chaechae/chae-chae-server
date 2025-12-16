package com.project.paymentservice.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundedEvent {

    private Long salesId;

    public static PaymentRefundedEvent of(Long salesId) {
        return PaymentRefundedEvent.builder()
                .salesId(salesId)
                .build();
    }
}

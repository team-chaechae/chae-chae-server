package com.project.paymentservice.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundedEvent {

    private String eventId;
    private String orderId;
    private Long salesId;
    private LocalDateTime refundedAt;

    public static PaymentRefundedEvent of(String orderId, Long salesId) {
        return PaymentRefundedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .salesId(salesId)
                .refundedAt(LocalDateTime.now())
                .build();
    }
}

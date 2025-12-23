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
public class PaymentCompletedEvent {

    private String eventId;
    private String orderId;
    private Long salesId;
    private Integer totalAmount;
    private LocalDateTime completedAt;

    public static PaymentCompletedEvent of(String orderId, Long salesId, Integer totalAmount) {
        return PaymentCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .salesId(salesId)
                .totalAmount(totalAmount)
                .completedAt(LocalDateTime.now())
                .build();
    }
}

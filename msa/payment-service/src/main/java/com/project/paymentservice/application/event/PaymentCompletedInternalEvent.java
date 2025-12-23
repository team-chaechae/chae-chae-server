package com.project.paymentservice.application.event;

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
public class PaymentCompletedInternalEvent {

    private String eventId;
    private String orderId;
    private Long salesId;
    private Integer totalAmount;
    private LocalDateTime completedAt;

    public static PaymentCompletedInternalEvent of(String orderId, Long salesId, Integer totalAmount) {
        return PaymentCompletedInternalEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .salesId(salesId)
                .totalAmount(totalAmount)
                .completedAt(LocalDateTime.now())
                .build();
    }

    public String getMessageKey() {
        return orderId;
    }

    public String getAggregateId() {
        return String.valueOf(salesId);
    }
}

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
public class PaymentRefundedInternalEvent {

    private String eventId;
    private String orderId;
    private Long salesId;
    private String reason;
    private LocalDateTime refundedAt;

    public static PaymentRefundedInternalEvent of(String orderId, Long salesId) {
        return of(orderId, salesId, null);
    }

    public static PaymentRefundedInternalEvent of(String orderId, Long salesId, String reason) {
        return PaymentRefundedInternalEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .salesId(salesId)
                .reason(reason)
                .refundedAt(LocalDateTime.now())
                .build();
    }

    public String getMessageKey() {
        return String.valueOf(salesId);
    }

    public String getAggregateId() {
        return String.valueOf(salesId);
    }
}

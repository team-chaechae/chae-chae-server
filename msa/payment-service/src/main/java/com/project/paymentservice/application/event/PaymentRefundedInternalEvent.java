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
    private LocalDateTime refundedAt;

    public static PaymentRefundedInternalEvent of(String orderId, Long salesId) {
        return PaymentRefundedInternalEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .salesId(salesId)
                .refundedAt(LocalDateTime.now())
                .build();
    }

    public String getMessageKey() {
        return orderId;  // orderId로 통일 → 같은 파티션 보장
    }

    public String getAggregateId() {
        return String.valueOf(salesId);
    }
}

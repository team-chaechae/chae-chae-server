package com.project.orderservice.application.event;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryCancelRequestedInternalEvent {

    private String eventId;
    private String orderId;
    private Long salesId;
    private String reason;
    private LocalDateTime requestedAt;

    public static DeliveryCancelRequestedInternalEvent of(String orderId, Long salesId, String reason) {
        return DeliveryCancelRequestedInternalEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .salesId(salesId)
                .reason(reason)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    public String getAggregateId() {
        return String.valueOf(salesId);
    }

    public String getMessageKey() {
        return orderId;
    }
}

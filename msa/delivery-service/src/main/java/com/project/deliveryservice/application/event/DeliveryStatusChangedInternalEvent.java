package com.project.deliveryservice.application.event;

import com.project.deliveryservice.domain.model.DeliveryEntity;
import com.project.deliveryservice.domain.model.constraint.DeliveryStatus;
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
public class DeliveryStatusChangedInternalEvent {

    private String eventId;
    private Long deliveryId;
    private Long salesId;
    private Long userId;
    private DeliveryStatus status;
    private String trackingNumber;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime occurredAt;

    public static DeliveryStatusChangedInternalEvent from(DeliveryEntity delivery) {
        return from(delivery, LocalDateTime.now());
    }

    public static DeliveryStatusChangedInternalEvent from(DeliveryEntity delivery, LocalDateTime occurredAt) {
        return DeliveryStatusChangedInternalEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .deliveryId(delivery.getId())
                .salesId(delivery.getSalesId())
                .userId(delivery.getUserId())
                .status(delivery.getStatus())
                .trackingNumber(delivery.getTrackingNumber())
                .shippedAt(delivery.getShippedAt())
                .deliveredAt(delivery.getDeliveredAt())
                .cancelledAt(delivery.getCancelledAt())
                .occurredAt(occurredAt)
                .build();
    }

    public String getAggregateId() {
        return String.valueOf(deliveryId);
    }

    public String getMessageKey() {
        return String.valueOf(salesId);
    }
}

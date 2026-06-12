package com.project.orderservice.infrastructure.kafka.dto;

import com.project.orderservice.domain.model.SalesDeliveryStatusType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatusChangedEvent {

    private String eventId;
    private Long deliveryId;
    private Long salesId;
    private Long userId;
    private SalesDeliveryStatusType status;
    private String trackingNumber;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime occurredAt;
}

package com.project.deliveryservice.infrastructure.kafka;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeliveryCreateRequestedEvent {

    private String eventId;
    private String orderId;
    private Long salesId;
    private Long userId;
    private String recipientName;
    private String recipientPhone;
    private String zipCode;
    private String address;
    private String addressDetail;
    private String deliveryMemo;
    private LocalDateTime requestedAt;
}

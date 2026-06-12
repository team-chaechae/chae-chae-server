package com.project.deliveryservice.infrastructure.kafka;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeliveryCancelRequestedEvent {

    private String eventId;
    private String orderId;
    private Long salesId;
    private String reason;
    private LocalDateTime requestedAt;
}

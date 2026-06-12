package com.project.orderservice.infrastructure.kafka;

import static org.mockito.Mockito.verify;

import com.project.orderservice.application.service.SalesDeliveryStatusProjectionService;
import com.project.orderservice.domain.model.SalesDeliveryStatusType;
import com.project.orderservice.infrastructure.kafka.dto.DeliveryStatusChangedEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.support.Acknowledgment;

class DeliveryStatusChangedEventConsumerTest {

    private final SalesDeliveryStatusProjectionService projectionService =
            Mockito.mock(SalesDeliveryStatusProjectionService.class);
    private final DeliveryStatusChangedEventConsumer consumer =
            new DeliveryStatusChangedEventConsumer(projectionService);

    @Test
    void handleDeliveryStatusChangedUpsertsProjectionAndAcks() {
        Acknowledgment acknowledgment = Mockito.mock(Acknowledgment.class);
        DeliveryStatusChangedEvent event = DeliveryStatusChangedEvent.builder()
                .eventId("event-1")
                .deliveryId(12L)
                .salesId(7L)
                .userId(3L)
                .status(SalesDeliveryStatusType.IN_TRANSIT)
                .trackingNumber("TRACK-1")
                .occurredAt(LocalDateTime.of(2026, 6, 11, 15, 20))
                .build();

        consumer.handleDeliveryStatusChanged(event, acknowledgment);

        verify(projectionService).upsert(event);
        verify(acknowledgment).acknowledge();
    }
}

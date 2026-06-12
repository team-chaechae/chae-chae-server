package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.application.service.SalesDeliveryStatusProjectionService;
import com.project.orderservice.infrastructure.kafka.dto.DeliveryStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryStatusChangedEventConsumer {

    private static final String TOPIC = "delivery-status-changed";

    private final SalesDeliveryStatusProjectionService projectionService;

    @KafkaListener(
            topics = TOPIC,
            groupId = "order-delivery-status-group",
            containerFactory = "deliveryStatusChangedListenerFactory",
            concurrency = "3"
    )
    public void handleDeliveryStatusChanged(DeliveryStatusChangedEvent event, Acknowledgment ack) {
        log.info("delivery_status_changed_received eventId={} salesId={} deliveryId={} status={}",
                event.getEventId(), event.getSalesId(), event.getDeliveryId(), event.getStatus());
        projectionService.upsert(event);
        ack.acknowledge();
    }
}

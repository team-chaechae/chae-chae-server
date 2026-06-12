package com.project.deliveryservice.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.deliveryservice.application.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryCancelRequestedConsumer {

    private static final String TOPIC = "delivery-cancel-requested";

    private final DeliveryService deliveryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TOPIC,
            groupId = "delivery-cancel-group",
            concurrency = "3"
    )
    public void handleDeliveryCancelRequested(String payload, Acknowledgment acknowledgment) throws Exception {
        DeliveryCancelRequestedEvent event = objectMapper.readValue(payload, DeliveryCancelRequestedEvent.class);
        try {
            log.info("delivery_cancel_requested_received orderId={} salesId={} eventId={}",
                    event.getOrderId(), event.getSalesId(), event.getEventId());
            deliveryService.cancelDeliveryBySalesId(event.getSalesId(), event.getOrderId(), event.getReason());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("delivery_cancel_requested_failed orderId={} salesId={} eventId={}",
                    event.getOrderId(), event.getSalesId(), event.getEventId(), e);
            throw e;
        }
    }
}

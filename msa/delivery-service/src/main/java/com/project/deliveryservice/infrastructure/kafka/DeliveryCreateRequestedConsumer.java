package com.project.deliveryservice.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.deliveryservice.application.service.DeliveryService;
import com.project.deliveryservice.presentation.request.ReqCreateDeliveryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryCreateRequestedConsumer {

    private static final String TOPIC = "delivery-create-requested";

    private final DeliveryService deliveryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TOPIC,
            groupId = "delivery-create-group",
            concurrency = "3"
    )
    public void handleDeliveryCreateRequested(String payload, Acknowledgment acknowledgment) throws Exception {
        DeliveryCreateRequestedEvent event = objectMapper.readValue(payload, DeliveryCreateRequestedEvent.class);
        try {
            log.info("delivery_create_requested_received orderId={} salesId={} eventId={}",
                    event.getOrderId(), event.getSalesId(), event.getEventId());
            deliveryService.createDeliveryFromEventIfAbsent(toRequest(event));
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("delivery_create_requested_failed orderId={} salesId={} eventId={}",
                    event.getOrderId(), event.getSalesId(), event.getEventId(), e);
            throw e;
        }
    }

    private ReqCreateDeliveryDTO toRequest(DeliveryCreateRequestedEvent event) {
        return ReqCreateDeliveryDTO.builder()
                .salesId(event.getSalesId())
                .userId(event.getUserId())
                .recipientName(event.getRecipientName())
                .recipientPhone(event.getRecipientPhone())
                .zipCode(event.getZipCode())
                .address(event.getAddress())
                .addressDetail(event.getAddressDetail())
                .deliveryMemo(event.getDeliveryMemo())
                .build();
    }
}

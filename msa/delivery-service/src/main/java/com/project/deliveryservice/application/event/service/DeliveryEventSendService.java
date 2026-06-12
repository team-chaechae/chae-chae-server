package com.project.deliveryservice.application.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.deliveryservice.application.event.DeliveryStatusChangedInternalEvent;
import com.project.deliveryservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.deliveryservice.domain.repository.OutboxRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryEventSendService {

    private static final String TOPIC_DELIVERY_STATUS_CHANGED = "delivery-status-changed";

    private final KafkaTemplate<String, String> stringKafkaTemplate;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void sendDeliveryStatusChanged(DeliveryStatusChangedInternalEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            stringKafkaTemplate.send(TOPIC_DELIVERY_STATUS_CHANGED, event.getMessageKey(), payload)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            markOutboxFail(event.getEventId(), throwable.getMessage());
                            log.warn("delivery_status_event_publish_failed eventId={} deliveryId={} salesId={} error={}",
                                    event.getEventId(), event.getDeliveryId(), event.getSalesId(), throwable.getMessage());
                            return;
                        }
                        markOutboxSuccess(event.getEventId());
                        log.info("delivery_status_event_published eventId={} deliveryId={} salesId={} status={}",
                                event.getEventId(), event.getDeliveryId(), event.getSalesId(), event.getStatus());
                    });
        } catch (JsonProcessingException e) {
            markOutboxFail(event.getEventId(), e.getMessage());
            throw new IllegalStateException("배송 상태 이벤트 직렬화 실패", e);
        }
    }

    private void markOutboxSuccess(String eventId) {
        outboxRepository.updateStatusSuccessByEventId(
                eventId,
                OutboxStatus.INIT,
                OutboxStatus.SEND_SUCCESS,
                LocalDateTime.now()
        );
    }

    private void markOutboxFail(String eventId, String errorMessage) {
        outboxRepository.updateStatusFailByEventId(
                eventId,
                OutboxStatus.INIT,
                OutboxStatus.SEND_FAIL,
                truncate(errorMessage),
                LocalDateTime.now()
        );
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}

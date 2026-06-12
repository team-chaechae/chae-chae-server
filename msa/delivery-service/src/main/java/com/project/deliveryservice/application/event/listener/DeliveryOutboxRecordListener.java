package com.project.deliveryservice.application.event.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.deliveryservice.application.event.DeliveryStatusChangedInternalEvent;
import com.project.deliveryservice.domain.model.OutboxEntity;
import com.project.deliveryservice.domain.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryOutboxRecordListener {

    private static final String AGGREGATE_TYPE_DELIVERY = "DELIVERY";
    private static final String EVENT_TYPE_DELIVERY_STATUS_CHANGED = "DELIVERY_STATUS_CHANGED";
    private static final String TOPIC_DELIVERY_STATUS_CHANGED = "delivery-status-changed";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void recordDeliveryStatusChangedOutbox(DeliveryStatusChangedInternalEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEntity outbox = OutboxEntity.create(
                    event.getEventId(),
                    AGGREGATE_TYPE_DELIVERY,
                    event.getAggregateId(),
                    EVENT_TYPE_DELIVERY_STATUS_CHANGED,
                    payload,
                    TOPIC_DELIVERY_STATUS_CHANGED,
                    event.getMessageKey()
            );
            outboxRepository.save(outbox);
            log.debug("delivery_outbox_recorded eventId={} deliveryId={} salesId={} status={}",
                    event.getEventId(), event.getDeliveryId(), event.getSalesId(), event.getStatus());
        } catch (JsonProcessingException e) {
            log.error("delivery_outbox_serialize_failed deliveryId={} salesId={}",
                    event.getDeliveryId(), event.getSalesId(), e);
            throw new IllegalStateException("배송 상태 이벤트 직렬화 실패", e);
        }
    }
}

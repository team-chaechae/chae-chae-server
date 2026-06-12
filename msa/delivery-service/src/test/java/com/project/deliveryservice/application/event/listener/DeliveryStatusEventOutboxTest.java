package com.project.deliveryservice.application.event.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.project.deliveryservice.application.event.DeliveryStatusChangedInternalEvent;
import com.project.deliveryservice.domain.model.OutboxEntity;
import com.project.deliveryservice.domain.model.constraint.DeliveryStatus;
import com.project.deliveryservice.domain.repository.OutboxRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("배송 상태 변경 Outbox")
class DeliveryStatusEventOutboxTest {

    @Test
    @DisplayName("배송 상태 변경 이벤트를 delivery-status-changed 토픽 Outbox로 기록한다")
    void recordDeliveryStatusChangedOutbox_SavesOutbox() {
        OutboxRepository outboxRepository = mock(OutboxRepository.class);
        DeliveryOutboxRecordListener listener = new DeliveryOutboxRecordListener(
                outboxRepository,
                JsonMapper.builder().findAndAddModules().build()
        );
        DeliveryStatusChangedInternalEvent event = DeliveryStatusChangedInternalEvent.builder()
                .eventId("event-1")
                .deliveryId(12L)
                .salesId(7L)
                .userId(3L)
                .status(DeliveryStatus.IN_TRANSIT)
                .trackingNumber("TRACK-1")
                .shippedAt(LocalDateTime.of(2026, 6, 11, 15, 20))
                .occurredAt(LocalDateTime.of(2026, 6, 11, 15, 20))
                .build();

        listener.recordDeliveryStatusChangedOutbox(event);

        ArgumentCaptor<OutboxEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEntity.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        OutboxEntity outbox = outboxCaptor.getValue();
        assertThat(outbox.getAggregateType()).isEqualTo("DELIVERY");
        assertThat(outbox.getAggregateId()).isEqualTo("12");
        assertThat(outbox.getEventType()).isEqualTo("DELIVERY_STATUS_CHANGED");
        assertThat(outbox.getTopic()).isEqualTo("delivery-status-changed");
        assertThat(outbox.getMessageKey()).isEqualTo("7");
        assertThat(outbox.getPayload()).contains("\"salesId\":7");
        assertThat(outbox.getPayload()).contains("\"status\":\"IN_TRANSIT\"");
    }
}

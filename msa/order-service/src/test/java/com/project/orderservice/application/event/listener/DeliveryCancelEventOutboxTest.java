package com.project.orderservice.application.event.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.orderservice.application.event.DeliveryCancelRequestedInternalEvent;
import com.project.orderservice.domain.model.OutboxEntity;
import com.project.orderservice.domain.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("배송 취소 요청 Outbox")
class DeliveryCancelEventOutboxTest {

    @Test
    @DisplayName("배송 취소 요청 이벤트를 delivery-cancel-requested 토픽 Outbox로 기록한다")
    void recordDeliveryCancelRequestedOutbox_SavesOutbox() {
        OutboxRepository outboxRepository = mock(OutboxRepository.class);
        OrderOutboxRecordListener listener = new OrderOutboxRecordListener(
                outboxRepository,
                new ObjectMapper().findAndRegisterModules()
        );
        DeliveryCancelRequestedInternalEvent event = DeliveryCancelRequestedInternalEvent.of(
                "order-7",
                7L,
                "환불 완료"
        );

        listener.recordDeliveryCancelRequestedOutbox(event);

        ArgumentCaptor<OutboxEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEntity.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        OutboxEntity outbox = outboxCaptor.getValue();
        assertThat(outbox.getAggregateType()).isEqualTo("ORDER");
        assertThat(outbox.getAggregateId()).isEqualTo("7");
        assertThat(outbox.getEventType()).isEqualTo("DELIVERY_CANCEL_REQUESTED");
        assertThat(outbox.getTopic()).isEqualTo("delivery-cancel-requested");
        assertThat(outbox.getMessageKey()).isEqualTo("order-7");
        assertThat(outbox.getPayload()).contains("\"salesId\":7");
        assertThat(outbox.getPayload()).contains("\"reason\":\"환불 완료\"");
    }
}

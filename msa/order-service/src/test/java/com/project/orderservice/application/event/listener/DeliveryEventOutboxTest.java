package com.project.orderservice.application.event.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.orderservice.application.event.DeliveryCreateRequestedInternalEvent;
import com.project.orderservice.domain.model.OutboxEntity;
import com.project.orderservice.domain.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("배송 생성 요청 Outbox")
class DeliveryEventOutboxTest {

    @Test
    @DisplayName("배송 생성 요청 이벤트를 delivery-create-requested 토픽 Outbox로 기록한다")
    void recordDeliveryCreateRequestedOutbox_SavesOutbox() {
        OutboxRepository outboxRepository = mock(OutboxRepository.class);
        OrderOutboxRecordListener listener = new OrderOutboxRecordListener(
                outboxRepository,
                new ObjectMapper().findAndRegisterModules()
        );
        DeliveryCreateRequestedInternalEvent event = deliveryEvent();

        listener.recordDeliveryCreateRequestedOutbox(event);

        ArgumentCaptor<OutboxEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEntity.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        OutboxEntity outbox = outboxCaptor.getValue();
        assertThat(outbox.getAggregateType()).isEqualTo("ORDER");
        assertThat(outbox.getAggregateId()).isEqualTo("7");
        assertThat(outbox.getEventType()).isEqualTo("DELIVERY_CREATE_REQUESTED");
        assertThat(outbox.getTopic()).isEqualTo("delivery-create-requested");
        assertThat(outbox.getMessageKey()).isEqualTo("order-7");
        assertThat(outbox.getPayload()).contains("\"salesId\":7");
        assertThat(outbox.getPayload()).contains("\"recipientName\":\"홍길동\"");
    }

    private DeliveryCreateRequestedInternalEvent deliveryEvent() {
        return DeliveryCreateRequestedInternalEvent.of(
                "order-7",
                7L,
                10L,
                "홍길동",
                "010-1234-5678",
                "12345",
                "서울시 강남구",
                "101동 1001호",
                "문 앞"
        );
    }
}

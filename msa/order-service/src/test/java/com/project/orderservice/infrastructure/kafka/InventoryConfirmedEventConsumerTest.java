package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.infrastructure.kafka.dto.InventoryConfirmedEvent;
import com.project.orderservice.infrastructure.sse.NotificationEvent;
import com.project.orderservice.infrastructure.sse.SseEmitterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * InventoryConfirmedEventConsumer 테스트
 *
 * 재고 차감 성공 이벤트 수신 시 SSE 알림 전송 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryConfirmedEventConsumer - SSE 알림 테스트")
class InventoryConfirmedEventConsumerTest {

    @Mock
    private SseEmitterRegistry sseEmitterRegistry;

    @Mock
    private Acknowledgment acknowledgment;

    private InventoryConfirmedEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new InventoryConfirmedEventConsumer(sseEmitterRegistry);
    }

    @Test
    @DisplayName("재고 확정 이벤트 수신 시 SSE PAYMENT_CONFIRMED 이벤트 전송")
    void handleInventoryConfirmed_ShouldSendSseEvent() {
        // given
        String orderId = "order-123";
        Long salesId = 1L;
        InventoryConfirmedEvent event = createTestEvent(orderId, salesId);

        // when
        consumer.handleInventoryConfirmed(event, acknowledgment);

        // then
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(sseEmitterRegistry, times(1)).sendEvent(eq(orderId), eventCaptor.capture());

        NotificationEvent sentEvent = eventCaptor.getValue();
        assertThat(sentEvent.getEventType()).isEqualTo("PAYMENT_CONFIRMED");
        assertThat(sentEvent.getOrderId()).isEqualTo(orderId);
        assertThat(sentEvent.getSalesId()).isEqualTo(salesId);
        assertThat(sentEvent.getMessage()).isEqualTo("결제가 완료되었습니다.");

        // acknowledge 호출 확인
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("SSE 전송 실패 시에도 acknowledge 호출")
    void handleInventoryConfirmed_WhenSseFails_ShouldStillAcknowledge() {
        // given
        String orderId = "order-456";
        Long salesId = 2L;
        InventoryConfirmedEvent event = createTestEvent(orderId, salesId);

        doThrow(new RuntimeException("SSE 전송 실패"))
                .when(sseEmitterRegistry).sendEvent(anyString(), any(NotificationEvent.class));

        // when
        consumer.handleInventoryConfirmed(event, acknowledgment);

        // then - SSE 실패해도 acknowledge는 호출되어야 함
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("orderId별로 올바른 SSE 이벤트 전송")
    void handleInventoryConfirmed_MultipleEvents_ShouldSendToCorrectOrderId() {
        // given
        InventoryConfirmedEvent event1 = createTestEvent("order-1", 1L);
        InventoryConfirmedEvent event2 = createTestEvent("order-2", 2L);
        InventoryConfirmedEvent event3 = createTestEvent("order-3", 3L);

        // when
        consumer.handleInventoryConfirmed(event1, acknowledgment);
        consumer.handleInventoryConfirmed(event2, acknowledgment);
        consumer.handleInventoryConfirmed(event3, acknowledgment);

        // then
        verify(sseEmitterRegistry).sendEvent(eq("order-1"), any(NotificationEvent.class));
        verify(sseEmitterRegistry).sendEvent(eq("order-2"), any(NotificationEvent.class));
        verify(sseEmitterRegistry).sendEvent(eq("order-3"), any(NotificationEvent.class));
        verify(acknowledgment, times(3)).acknowledge();
    }

    private InventoryConfirmedEvent createTestEvent(String orderId, Long salesId) {
        return InventoryConfirmedEvent.builder()
                .eventId("event-" + System.currentTimeMillis())
                .orderId(orderId)
                .salesId(salesId)
                .confirmedAt(LocalDateTime.now())
                .build();
    }
}

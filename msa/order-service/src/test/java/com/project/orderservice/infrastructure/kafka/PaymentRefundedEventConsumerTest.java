package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.application.service.SalesService;
import com.project.orderservice.infrastructure.alert.SlackAlertService;
import com.project.orderservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.orderservice.infrastructure.kafka.dto.PaymentRefundedEvent;
import com.project.orderservice.infrastructure.sse.NotificationEvent;
import com.project.orderservice.infrastructure.sse.SseEmitterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PaymentRefundedEventConsumer 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRefundedEventConsumer - DLQ 동작 테스트")
class PaymentRefundedEventConsumerTest {

    @Mock
    private SalesService salesService;

    @Mock
    private SseEmitterRegistry sseEmitterRegistry;

    @Mock
    private Acknowledgment acknowledgment;

    @Mock
    private SlackAlertService slackAlertService;

    private PaymentRefundedEventConsumer consumer;
    private BlockingThreadPoolExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new BlockingThreadPoolExecutor(
                2, 4, 10, 5000,
                new LinkedBlockingQueue<>(100),
                Executors.defaultThreadFactory()
        );
        consumer = new PaymentRefundedEventConsumer(salesService, executor, sseEmitterRegistry, slackAlertService);
    }

    @Test
    @DisplayName("정상 처리 시 SSE 전송 및 ack")
    void handlePaymentRefunded_ShouldSendSseEvent() {
        // given
        String orderId = "order-123";
        Long salesId = 1L;
        PaymentRefundedEvent event = createTestEvent(orderId, salesId);

        // when
        consumer.handlePaymentRefunded(event, acknowledgment);

        // then
        verify(salesService, times(1)).cancelSales(eq(salesId), eq(orderId), anyString());
        verify(sseEmitterRegistry, times(1)).sendEvent(eq(orderId), any(NotificationEvent.class));
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("SalesService 실패 시 예외 전파 및 ack 없음")
    void handlePaymentRefunded_WhenSalesServiceFails_ShouldThrow() {
        // given
        String orderId = "order-456";
        Long salesId = 2L;
        PaymentRefundedEvent event = createTestEvent(orderId, salesId);

        doThrow(new RuntimeException("DB 연결 실패"))
                .when(salesService).cancelSales(anyLong(), anyString(), anyString());

        // when & then
        assertThatThrownBy(() -> consumer.handlePaymentRefunded(event, acknowledgment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB 연결 실패");

        verify(acknowledgment, never()).acknowledge();
    }

    private PaymentRefundedEvent createTestEvent(String orderId, Long salesId) {
        return PaymentRefundedEvent.builder()
                .eventId("event-" + System.currentTimeMillis())
                .orderId(orderId)
                .salesId(salesId)
                .refundedAt(LocalDateTime.now())
                .build();
    }
}

package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.application.service.SalesService;
import com.project.orderservice.infrastructure.alert.SlackAlertService;
import com.project.orderservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.orderservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * PaymentCompletedEventConsumer 테스트
 *
 * 목적: Consumer 성공/실패 시 ACK 및 예외 전파 동작 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCompletedEventConsumer - DLQ 동작 테스트")
class PaymentCompletedEventConsumerTest {

    @Mock
    private SalesService salesService;

    @Mock
    private Acknowledgment acknowledgment;

    @Mock
    private SlackAlertService slackAlertService;

    private PaymentCompletedEventConsumer consumer;
    private BlockingThreadPoolExecutor executor;

    @BeforeEach
    void setUp() {
        // 동기 실행을 위한 단순한 executor 설정
        executor = new BlockingThreadPoolExecutor(
                2, 4, 10, 5000,
                new java.util.concurrent.LinkedBlockingQueue<>(100),
                java.util.concurrent.Executors.defaultThreadFactory()
        );
        consumer = new PaymentCompletedEventConsumer(
                salesService,
                executor,
                slackAlertService,
                new SimpleMeterRegistry()
        );
    }

    @Test
    @DisplayName("SalesService 예외 발생 시 RuntimeException으로 전파")
    void handlePaymentCompleted_WhenSalesServiceThrows_ShouldThrow() {
        // given
        PaymentCompletedEvent event = createTestEvent("order-123", 1L, 10000);
        RuntimeException testException = new RuntimeException("DB 연결 실패");

        doThrow(testException).when(salesService).completeSales(anyLong(), anyString());

        // when & then
        assertThatThrownBy(() -> consumer.handlePaymentCompleted(event, acknowledgment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB 연결 실패");

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("정상 처리 시 ACK 호출")
    void handlePaymentCompleted_WhenSuccess_ShouldAck() {
        // given
        PaymentCompletedEvent event = createTestEvent("order-456", 2L, 20000);

        doNothing().when(salesService).completeSales(anyLong(), anyString());

        // when
        consumer.handlePaymentCompleted(event, acknowledgment);

        // then
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    private PaymentCompletedEvent createTestEvent(String orderId, Long salesId, Integer totalAmount) {
        return PaymentCompletedEvent.builder()
                .eventId("event-" + System.currentTimeMillis())
                .orderId(orderId)
                .salesId(salesId)
                .totalAmount(totalAmount)
                .completedAt(LocalDateTime.now())
                .build();
    }
}

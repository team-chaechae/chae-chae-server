package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.application.service.PaymentOrchestrationService;
import com.project.orderservice.infrastructure.alert.SlackAlertService;
import com.project.orderservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.orderservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PaymentCompletedEventConsumer 테스트
 *
 * 목적: Consumer에서 에러 발생 시 SlackAlertService가 호출되는지 검증
 * 실제 Slack 웹훅은 사용하지 않고 Mock으로 호출 여부만 확인
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCompletedEventConsumer - DLQ/Slack 알림 테스트")
class PaymentCompletedEventConsumerTest {

    @Mock
    private PaymentOrchestrationService paymentOrchestrationService;

    @Mock
    private SlackAlertService slackAlertService;

    @Mock
    private Acknowledgment acknowledgment;

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
        consumer = new PaymentCompletedEventConsumer(paymentOrchestrationService, executor, slackAlertService);
    }

    @Test
    @DisplayName("오케스트레이션 예외 발생 시 SlackAlertService.sendKafkaErrorAlert() 호출됨")
    void handlePaymentCompleted_WhenOrchestrationFails_ShouldSendSlackAlert() throws Exception {
        // given
        PaymentCompletedEvent event = createTestEvent("order-123", 1L, 10000);
        RuntimeException testException = new RuntimeException("DB 연결 실패");

        doThrow(testException).when(paymentOrchestrationService).handlePaymentCompleted(any(PaymentCompletedEvent.class));

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(slackAlertService).sendKafkaErrorAlert(anyString(), anyString(), any(Exception.class));

        // when
        consumer.handlePaymentCompleted(event, acknowledgment);

        // then - 비동기 작업 완료 대기
        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // Slack 알림 호출 검증
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        verify(slackAlertService, times(1))
                .sendKafkaErrorAlert(topicCaptor.capture(), messageCaptor.capture(), exceptionCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("payment-completed");
        assertThat(messageCaptor.getValue()).contains("order-123");
        assertThat(messageCaptor.getValue()).contains("salesId: 1");
        assertThat(exceptionCaptor.getValue().getMessage()).isEqualTo("DB 연결 실패");

        // 실패 시 ack하지 않아 offset commit을 막고 재처리 가능 상태로 둔다.
        verify(acknowledgment, after(300).never()).acknowledge();
    }

    @Test
    @DisplayName("정상 처리 시 SlackAlertService 호출되지 않음")
    void handlePaymentCompleted_WhenSuccess_ShouldNotSendSlackAlert() throws Exception {
        // given
        PaymentCompletedEvent event = createTestEvent("order-456", 2L, 20000);

        doNothing().when(paymentOrchestrationService).handlePaymentCompleted(any(PaymentCompletedEvent.class));

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(acknowledgment).acknowledge();

        // when
        consumer.handlePaymentCompleted(event, acknowledgment);

        // then - 비동기 작업 완료 대기
        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // Slack 알림 호출되지 않아야 함
        verify(slackAlertService, never()).sendKafkaErrorAlert(anyString(), anyString(), any(Exception.class));

        // acknowledge는 호출되어야 함
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("재고 부족 예외 발생 시 Slack 알림에 상세 정보 포함")
    void handlePaymentCompleted_WhenInsufficientStock_ShouldIncludeDetailInSlackAlert() throws Exception {
        // given
        PaymentCompletedEvent event = createTestEvent("order-789", 3L, 50000);
        RuntimeException stockException = new RuntimeException("재고 부족: productId=100, 요청=10, 재고=5");

        doThrow(stockException).when(paymentOrchestrationService).handlePaymentCompleted(any(PaymentCompletedEvent.class));

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(slackAlertService).sendKafkaErrorAlert(anyString(), anyString(), any(Exception.class));

        // when
        consumer.handlePaymentCompleted(event, acknowledgment);

        // then
        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(slackAlertService).sendKafkaErrorAlert(eq("payment-completed"), anyString(), exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue().getMessage()).contains("재고 부족");
    }

    @Test
    @DisplayName("NullPointerException 발생 시에도 Slack 알림 전송")
    void handlePaymentCompleted_WhenNPE_ShouldSendSlackAlert() throws Exception {
        // given
        PaymentCompletedEvent event = createTestEvent("order-npe", 4L, 15000);
        NullPointerException npe = new NullPointerException("salesId가 null입니다");

        doThrow(npe).when(paymentOrchestrationService).handlePaymentCompleted(any(PaymentCompletedEvent.class));

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(slackAlertService).sendKafkaErrorAlert(anyString(), anyString(), any(Exception.class));

        // when
        consumer.handlePaymentCompleted(event, acknowledgment);

        // then
        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        verify(slackAlertService, times(1))
                .sendKafkaErrorAlert(eq("payment-completed"), contains("order-npe"), any(NullPointerException.class));
    }

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

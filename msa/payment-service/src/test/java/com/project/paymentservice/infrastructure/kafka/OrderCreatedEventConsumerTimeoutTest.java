package com.project.paymentservice.infrastructure.kafka;

import com.project.common.dlq.domain.DlqMessage;
import com.project.common.dlq.exception.DlqExceptionClassifier;
import com.project.paymentservice.application.event.PaymentCompletedInternalEvent;
import com.project.paymentservice.application.service.PaymentService;
import com.project.paymentservice.infrastructure.alert.SlackAlertService;
import com.project.paymentservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.paymentservice.infrastructure.kafka.dto.OrderCreatedEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCreatedEventConsumer Timeout DLQ 재현 테스트")
class OrderCreatedEventConsumerTimeoutTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private SlackAlertService slackAlertService;

    @Mock
    private Acknowledgment acknowledgment;

    private BlockingThreadPoolExecutor executor;

    @AfterEach
    void tearDown() throws Exception {
        if (executor != null) {
            executor.destroy();
        }
    }

    @Test
    @DisplayName("결제 처리 중 TimeoutException이 발생하면 ack하지 않고 DLQ 대상 기술 오류로 전파한다")
    void handleOrderCreated_PropagatesTimeoutExceptionForDlq_WhenPaymentProcessingTimeouts() {
        // given
        executor = new BlockingThreadPoolExecutor(
                1, 1, 1, 1000,
                new LinkedBlockingQueue<>(10),
                Executors.defaultThreadFactory()
        );
        OrderCreatedEventConsumer consumer = new OrderCreatedEventConsumer(
                paymentService,
                executor,
                slackAlertService,
                new SimpleMeterRegistry()
        );
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId("order-timeout")
                .salesId(100L)
                .totalAmount(10000)
                .items(List.of(OrderCreatedEvent.OrderItem.builder()
                        .productId(1L)
                        .productName("product")
                        .quantity(1)
                        .price(10000)
                        .build()))
                .build();
        RuntimeException timeoutFailure = new RuntimeException(
                new TimeoutException("payment processing timed out"));

        doThrow(timeoutFailure).when(paymentService).processPayment(
                anyString(),
                anyLong(),
                anyInt(),
                anyList()
        );

        // when & then
        assertThatThrownBy(() -> consumer.handleOrderCreated(event, acknowledgment))
                .isInstanceOf(RuntimeException.class)
                .satisfies(thrown -> {
                    assertThat(rootCause(thrown)).isInstanceOf(TimeoutException.class);
                    DlqExceptionClassifier classifier = new DlqExceptionClassifier();
                    assertThat(classifier.classify(thrown))
                            .isEqualTo(DlqMessage.ExceptionCategory.TECHNICAL);
                });

        verify(acknowledgment, never()).acknowledge();
        verify(slackAlertService).sendKafkaErrorAlert(anyString(), anyString(), any(Exception.class));
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}

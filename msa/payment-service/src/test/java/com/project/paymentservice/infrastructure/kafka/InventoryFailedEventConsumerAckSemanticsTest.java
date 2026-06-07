package com.project.paymentservice.infrastructure.kafka;

import com.project.paymentservice.application.service.PaymentService;
import com.project.paymentservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.paymentservice.infrastructure.kafka.dto.InventoryFailedEvent;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryFailedEventConsumer ack 의미론 테스트")
class InventoryFailedEventConsumerAckSemanticsTest {

    @Mock
    private PaymentService paymentService;

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
    @DisplayName("환불 처리 성공 시 ack한다")
    void handleInventoryFailed_Acknowledges_WhenRefundSucceeds() {
        // given
        executor = newExecutor();
        InventoryFailedEventConsumer consumer = new InventoryFailedEventConsumer(paymentService, executor);
        InventoryFailedEvent event = testEvent();

        // when
        consumer.handleInventoryFailed(event, acknowledgment);

        // then
        verify(paymentService, timeout(1000)).refundPayment(1L, "재고 부족");
        verify(acknowledgment, timeout(1000)).acknowledge();
    }

    @Test
    @DisplayName("환불 처리 실패 시 ack하지 않아 재처리 가능 상태로 둔다")
    void handleInventoryFailed_DoesNotAcknowledge_WhenRefundFails() {
        // given
        executor = newExecutor();
        InventoryFailedEventConsumer consumer = new InventoryFailedEventConsumer(paymentService, executor);
        InventoryFailedEvent event = testEvent();
        doThrow(new RuntimeException("Toss cancel timeout"))
                .when(paymentService).refundPayment(anyLong(), anyString());

        // when
        consumer.handleInventoryFailed(event, acknowledgment);

        // then
        verify(paymentService, timeout(1000)).refundPayment(1L, "재고 부족");
        verify(acknowledgment, after(300).never()).acknowledge();
    }

    private BlockingThreadPoolExecutor newExecutor() {
        return new BlockingThreadPoolExecutor(
                1,
                1,
                1,
                1000,
                new LinkedBlockingQueue<>(10),
                Executors.defaultThreadFactory()
        );
    }

    private InventoryFailedEvent testEvent() {
        return InventoryFailedEvent.builder()
                .orderId("order-inventory-failed")
                .salesId(1L)
                .reason("재고 부족")
                .failedAt(LocalDateTime.now())
                .build();
    }
}

package com.project.inventoryservice.infrastructure.kafka;

import com.project.inventoryservice.application.service.InventoryDeductionService;
import com.project.inventoryservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.inventoryservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCompletedEventConsumer ack 의미론 테스트")
class PaymentCompletedEventConsumerAckSemanticsTest {

    @Mock
    private InventoryDeductionService inventoryDeductionService;

    @Mock
    private Acknowledgment acknowledgment;

    private BlockingThreadPoolExecutor executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("재고 차감 성공 시에만 ack한다")
    void handlePaymentCompleted_Acknowledges_WhenInventoryDeductionSucceeds() {
        // given
        executor = newExecutor();
        PaymentCompletedEventConsumer consumer = new PaymentCompletedEventConsumer(
                inventoryDeductionService,
                executor
        );
        PaymentCompletedEvent event = testEvent();

        // when
        consumer.handlePaymentCompleted(event, acknowledgment);

        // then
        verify(inventoryDeductionService, timeout(1000)).processPaymentCompleted(event);
        verify(acknowledgment, timeout(1000)).acknowledge();
    }

    @Test
    @DisplayName("재고 차감 처리 중 예외가 발생하면 ack하지 않아 재처리 가능 상태로 둔다")
    void handlePaymentCompleted_DoesNotAcknowledge_WhenInventoryDeductionFails() {
        // given
        executor = newExecutor();
        PaymentCompletedEventConsumer consumer = new PaymentCompletedEventConsumer(
                inventoryDeductionService,
                executor
        );
        PaymentCompletedEvent event = testEvent();
        CountDownLatch serviceCalled = new CountDownLatch(1);

        willAnswer(invocation -> {
            serviceCalled.countDown();
            throw new RuntimeException("Redis timeout");
        }).given(inventoryDeductionService).processPaymentCompleted(event);

        // when
        consumer.handlePaymentCompleted(event, acknowledgment);

        // then
        await().atMost(Duration.ofSeconds(1)).until(() -> serviceCalled.getCount() == 0);
        await().during(Duration.ofMillis(300))
                .atMost(Duration.ofMillis(600))
                .untilAsserted(() -> verify(acknowledgment, never()).acknowledge());
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

    private PaymentCompletedEvent testEvent() {
        return PaymentCompletedEvent.builder()
                .eventId("event-ack-semantics")
                .orderId("order-ack-semantics")
                .salesId(1L)
                .totalAmount(10000)
                .items(List.of(PaymentCompletedEvent.OrderItem.builder()
                        .productId(1999L)
                        .productName("상품_1999")
                        .quantity(1)
                        .price(10000)
                        .build()))
                .completedAt(LocalDateTime.now())
                .build();
    }
}

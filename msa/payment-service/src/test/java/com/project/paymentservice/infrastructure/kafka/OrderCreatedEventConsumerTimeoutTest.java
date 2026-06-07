package com.project.paymentservice.infrastructure.kafka;

import com.project.paymentservice.application.service.PaymentService;
import com.project.paymentservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.paymentservice.infrastructure.kafka.dto.OrderCreatedEvent;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCreatedEventConsumer Timeout 처리 테스트")
class OrderCreatedEventConsumerTimeoutTest {

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
    @DisplayName("결제 처리 중 TimeoutException이 발생하면 ack하지 않아 재처리 가능 상태로 둔다")
    void handleOrderCreated_DoesNotAcknowledge_WhenPaymentProcessingTimeouts() {
        // given
        executor = new BlockingThreadPoolExecutor(
                1, 1, 1, 1000,
                new LinkedBlockingQueue<>(10),
                Executors.defaultThreadFactory()
        );
        OrderCreatedEventConsumer consumer = new OrderCreatedEventConsumer(
                paymentService,
                executor
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

        // when
        consumer.handleOrderCreated(event, acknowledgment);

        // then
        verify(paymentService, timeout(1000)).processPayment(anyString(), anyLong(), anyInt(), anyList());
        verify(acknowledgment, after(300).never()).acknowledge();
    }
}

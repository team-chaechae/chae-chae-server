package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.application.service.SalesService;
import com.project.orderservice.infrastructure.alert.SlackAlertService;
import com.project.orderservice.application.response.ResSalesGetByIdDTO;
import com.project.orderservice.infrastructure.client.InventoryFeignClient;
import com.project.orderservice.infrastructure.client.dto.InventoryChangeDTO;
import com.project.orderservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.orderservice.infrastructure.kafka.dto.PaymentRefundedEvent;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * PaymentRefundedEventConsumer 테스트
 *
 * 결제 환불 이벤트 수신 시:
 * 1. SalesService.cancelSales() 호출
 * 2. SSE INVENTORY_FAILED 이벤트 전송
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRefundedEventConsumer - SSE 알림 테스트")
class PaymentRefundedEventConsumerTest {

    @Mock
    private SalesService salesService;

    @Mock
    private SlackAlertService slackAlertService;

    @Mock
    private SseEmitterRegistry sseEmitterRegistry;

    @Mock
    private Acknowledgment acknowledgment;

    @Mock
    private InventoryFeignClient inventoryFeignClient;

    private PaymentRefundedEventConsumer consumer;
    private BlockingThreadPoolExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new BlockingThreadPoolExecutor(
                2, 4, 10, 5000,
                new LinkedBlockingQueue<>(100),
                Executors.defaultThreadFactory()
        );
        consumer = new PaymentRefundedEventConsumer(
                salesService, executor, slackAlertService, sseEmitterRegistry, inventoryFeignClient
        );
    }

    @Test
    @DisplayName("환불 이벤트 수신 시 SSE INVENTORY_FAILED 이벤트 전송")
    void handlePaymentRefunded_ShouldSendSseEvent() throws Exception {
        // given
        String orderId = "order-123";
        Long salesId = 1L;
        PaymentRefundedEvent event = createTestEvent(orderId, salesId);

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(acknowledgment).acknowledge();

        // when
        consumer.handlePaymentRefunded(event, acknowledgment);

        // then - 비동기 작업 완료 대기
        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // SSE 이벤트 전송 검증
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(sseEmitterRegistry, times(1)).sendEvent(eq(orderId), eventCaptor.capture());

        NotificationEvent sentEvent = eventCaptor.getValue();
        assertThat(sentEvent.getEventType()).isEqualTo("INVENTORY_FAILED");
        assertThat(sentEvent.getOrderId()).isEqualTo(orderId);
        assertThat(sentEvent.getSalesId()).isEqualTo(salesId);
        assertThat(sentEvent.getMessage()).contains("재고 부족");

        // SalesService.cancelSales() 호출 확인
        verify(salesService, times(1)).cancelSales(eq(salesId), eq(orderId), anyString());
        verifyNoInteractions(inventoryFeignClient);
    }

    @Test
    @DisplayName("고객 요청 환불 이벤트 수신 시 재고를 복구한다")
    void handlePaymentRefunded_WhenCustomerCancel_ShouldRestoreInventory() throws Exception {
        // given
        String orderId = "order-cancel";
        Long salesId = 10L;
        PaymentRefundedEvent event = createTestEvent(orderId, salesId, "고객 요청");
        given(salesService.getSalesBySalesId(salesId)).willReturn(ResSalesGetByIdDTO.builder()
                .sales(ResSalesGetByIdDTO.SalesDetail.builder()
                        .salesId(salesId)
                        .orderId(orderId)
                        .status("COMPLETED")
                        .items(List.of(ResSalesGetByIdDTO.SalesItemDetail.builder()
                                .productId(1999L)
                                .quantity(1)
                                .build()))
                        .build())
                .build());
        given(inventoryFeignClient.increaseInventory(any(InventoryChangeDTO.Request.class)))
                .willReturn(InventoryChangeDTO.Response.builder().success(true).build());

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(acknowledgment).acknowledge();

        // when
        consumer.handlePaymentRefunded(event, acknowledgment);

        // then
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        verify(salesService).cancelSales(salesId, orderId, "고객 요청");
        ArgumentCaptor<InventoryChangeDTO.Request> requestCaptor =
                ArgumentCaptor.forClass(InventoryChangeDTO.Request.class);
        verify(inventoryFeignClient).increaseInventory(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getItems()).hasSize(1);
        assertThat(requestCaptor.getValue().getItems().get(0).getProductId()).isEqualTo(1999L);
        assertThat(requestCaptor.getValue().getItems().get(0).getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("고객 요청 환불이어도 주문이 완료 전이면 재고 복구를 스킵한다")
    void handlePaymentRefunded_WhenSalesNotCompleted_ShouldSkipInventoryRestore() throws Exception {
        // given
        String orderId = "order-cancel-pending";
        Long salesId = 11L;
        PaymentRefundedEvent event = createTestEvent(orderId, salesId, "고객 요청");
        given(salesService.getSalesBySalesId(salesId)).willReturn(ResSalesGetByIdDTO.builder()
                .sales(ResSalesGetByIdDTO.SalesDetail.builder()
                        .salesId(salesId)
                        .orderId(orderId)
                        .status("PENDING")
                        .items(List.of(ResSalesGetByIdDTO.SalesItemDetail.builder()
                                .productId(1999L)
                                .quantity(1)
                                .build()))
                        .build())
                .build());

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(acknowledgment).acknowledge();

        // when
        consumer.handlePaymentRefunded(event, acknowledgment);

        // then
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        verify(salesService).cancelSales(salesId, orderId, "고객 요청");
        verifyNoInteractions(inventoryFeignClient);
    }

    @Test
    @DisplayName("SalesService 실패 시 ack하지 않아 재처리 가능 상태로 둔다")
    void handlePaymentRefunded_WhenSalesServiceFails_ShouldNotAcknowledge() throws Exception {
        // given
        String orderId = "order-456";
        Long salesId = 2L;
        PaymentRefundedEvent event = createTestEvent(orderId, salesId);

        doThrow(new RuntimeException("DB 연결 실패"))
                .when(salesService).cancelSales(anyLong(), anyString(), anyString());

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(slackAlertService).sendKafkaErrorAlert(anyString(), anyString(), any(Exception.class));

        // when
        consumer.handlePaymentRefunded(event, acknowledgment);

        // then
        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // Slack 알림 호출 확인
        verify(slackAlertService, times(1))
                .sendKafkaErrorAlert(eq("payment-refunded"), contains(orderId), any(Exception.class));

        // 실패 시 ack하지 않아 offset commit을 막고 재처리 가능 상태로 둔다.
        verify(acknowledgment, after(300).never()).acknowledge();
    }

    @Test
    @DisplayName("SSE 전송 실패 시에도 나머지 로직 정상 동작")
    void handlePaymentRefunded_WhenSseFails_ShouldContinue() throws Exception {
        // given
        String orderId = "order-789";
        Long salesId = 3L;
        PaymentRefundedEvent event = createTestEvent(orderId, salesId);

        // SSE 전송 시 예외 발생
        doThrow(new RuntimeException("SSE 전송 실패"))
                .when(sseEmitterRegistry).sendEvent(anyString(), any(NotificationEvent.class));

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(acknowledgment).acknowledge();

        // when
        consumer.handlePaymentRefunded(event, acknowledgment);

        // then
        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // SalesService는 SSE 전에 호출되므로 정상 호출됨
        verify(salesService, times(1)).cancelSales(eq(salesId), eq(orderId), anyString());
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("여러 환불 이벤트 순차 처리")
    void handlePaymentRefunded_MultipleEvents_ShouldProcessSequentially() throws Exception {
        // given
        PaymentRefundedEvent event1 = createTestEvent("order-1", 1L);
        PaymentRefundedEvent event2 = createTestEvent("order-2", 2L);

        CountDownLatch latch = new CountDownLatch(2);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(acknowledgment).acknowledge();

        // when
        consumer.handlePaymentRefunded(event1, acknowledgment);
        consumer.handlePaymentRefunded(event2, acknowledgment);

        // then
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        verify(sseEmitterRegistry).sendEvent(eq("order-1"), any(NotificationEvent.class));
        verify(sseEmitterRegistry).sendEvent(eq("order-2"), any(NotificationEvent.class));
        verify(salesService).cancelSales(eq(1L), eq("order-1"), anyString());
        verify(salesService).cancelSales(eq(2L), eq("order-2"), anyString());
    }

    private PaymentRefundedEvent createTestEvent(String orderId, Long salesId) {
        return createTestEvent(orderId, salesId, null);
    }

    private PaymentRefundedEvent createTestEvent(String orderId, Long salesId, String reason) {
        return PaymentRefundedEvent.builder()
                .eventId("event-" + System.currentTimeMillis())
                .orderId(orderId)
                .salesId(salesId)
                .reason(reason)
                .refundedAt(LocalDateTime.now())
                .build();
    }
}

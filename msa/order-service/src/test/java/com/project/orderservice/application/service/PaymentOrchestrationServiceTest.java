package com.project.orderservice.application.service;

import com.project.orderservice.infrastructure.client.InventoryFeignClient;
import com.project.orderservice.infrastructure.client.PaymentFeignClient;
import com.project.orderservice.infrastructure.client.dto.InventoryChangeDTO;
import com.project.orderservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import com.project.orderservice.domain.model.PaymentOrchestrationEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentOrchestrationService")
class PaymentOrchestrationServiceTest {

    @Mock
    private SalesService salesService;

    @Mock
    private InventoryFeignClient inventoryFeignClient;

    @Mock
    private PaymentFeignClient paymentFeignClient;

    @Mock
    private PaymentOrchestrationStateService orchestrationStateService;

    @Test
    @DisplayName("결제 완료 이벤트를 받으면 재고 차감 후 주문을 완료한다")
    void handlePaymentCompleted_DecreasesInventoryThenCompletesSales() {
        // given
        PaymentOrchestrationService service = service();
        PaymentCompletedEvent event = paymentCompletedEvent();
        givenStartedOrchestration();
        given(inventoryFeignClient.decreaseInventory(any(InventoryChangeDTO.Request.class)))
                .willReturn(InventoryChangeDTO.Response.builder()
                        .success(true)
                        .processedCount(2)
                        .build());

        // when
        service.handlePaymentCompleted(event);

        // then
        ArgumentCaptor<InventoryChangeDTO.Request> requestCaptor =
                ArgumentCaptor.forClass(InventoryChangeDTO.Request.class);
        verify(inventoryFeignClient).decreaseInventory(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getItems()).hasSize(2);
        assertThat(requestCaptor.getValue().getItems().get(0).getProductId()).isEqualTo(1999L);
        assertThat(requestCaptor.getValue().getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(requestCaptor.getValue().getItems().get(1).getProductId()).isEqualTo(2000L);
        assertThat(requestCaptor.getValue().getItems().get(1).getQuantity()).isEqualTo(1);

        verify(salesService).completeSales(7L, "order-7");
        verify(paymentFeignClient, never()).refundPayment(any(), any());
        verify(salesService, never()).cancelSales(any(), any(), any());
    }

    @Test
    @DisplayName("재고 차감 실패 시 결제를 환불하고 주문을 취소한다")
    void handlePaymentCompleted_WhenInventoryDecreaseFails_RefundsPaymentAndCancelsSales() {
        // given
        PaymentOrchestrationService service = service();
        PaymentCompletedEvent event = paymentCompletedEvent();
        givenStartedOrchestration();
        given(inventoryFeignClient.decreaseInventory(any(InventoryChangeDTO.Request.class)))
                .willThrow(new RuntimeException("재고 부족"));

        // when
        service.handlePaymentCompleted(event);

        // then
        verify(paymentFeignClient).refundPayment(eq(7L), contains("재고 차감 실패"));
        verify(salesService).cancelSales(eq(7L), eq("order-7"), contains("재고 차감 실패"));
        verify(salesService, never()).completeSales(any(), any());
    }

    @Test
    @DisplayName("재고 차감 응답이 실패이면 결제를 환불하고 주문을 취소한다")
    void handlePaymentCompleted_WhenInventoryResponseFails_RefundsPaymentAndCancelsSales() {
        // given
        PaymentOrchestrationService service = service();
        PaymentCompletedEvent event = paymentCompletedEvent();
        givenStartedOrchestration();
        given(inventoryFeignClient.decreaseInventory(any(InventoryChangeDTO.Request.class)))
                .willReturn(InventoryChangeDTO.Response.builder()
                        .success(false)
                        .processedCount(0)
                        .build());

        // when
        service.handlePaymentCompleted(event);

        // then
        verify(paymentFeignClient).refundPayment(eq(7L), contains("재고 차감 실패"));
        verify(salesService).cancelSales(eq(7L), eq("order-7"), contains("재고 차감 실패"));
        verify(salesService, never()).completeSales(any(), any());
    }

    @Test
    @DisplayName("재고 차감 실패 후 환불이 실패해도 주문 취소를 먼저 반영하고 재처리를 유도한다")
    void handlePaymentCompleted_WhenRefundFailsDuringInventoryCompensation_CancelsSalesAndRethrows() {
        // given
        PaymentOrchestrationService service = service();
        PaymentCompletedEvent event = paymentCompletedEvent();
        givenStartedOrchestration();
        given(inventoryFeignClient.decreaseInventory(any(InventoryChangeDTO.Request.class)))
                .willThrow(new RuntimeException("재고 부족"));
        doThrow(new RuntimeException("payment unavailable"))
                .when(paymentFeignClient).refundPayment(eq(7L), any(String.class));

        // when & then
        assertThatThrownBy(() -> service.handlePaymentCompleted(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("payment unavailable");

        verify(paymentFeignClient).refundPayment(eq(7L), contains("재고 차감 실패"));
        verify(salesService).cancelSales(eq(7L), eq("order-7"), contains("재고 차감 실패"));
        verify(orchestrationStateService).markOrderCancelled(any(PaymentOrchestrationEntity.class));
        verify(salesService, never()).completeSales(any(), any());
    }

    @Test
    @DisplayName("주문 취소는 됐지만 환불이 남은 보상 이벤트는 환불만 이어서 처리한다")
    void handlePaymentCompleted_WhenOrderCancelledButRefundPending_RetriesRefundOnly() {
        // given
        PaymentOrchestrationService service = service();
        PaymentCompletedEvent event = paymentCompletedEvent();
        PaymentOrchestrationEntity orchestration = PaymentOrchestrationEntity.start("order-7", 7L);
        orchestration.startCompensation("재고 차감 실패");
        orchestration.markOrderCancelled();

        given(orchestrationStateService.getOrCreate("order-7", 7L)).willReturn(orchestration);
        givenStateTransitionsReturnSameEntity();

        // when
        service.handlePaymentCompleted(event);

        // then
        verify(inventoryFeignClient, never()).decreaseInventory(any(InventoryChangeDTO.Request.class));
        verify(inventoryFeignClient, never()).increaseInventory(any(InventoryChangeDTO.Request.class));
        verify(paymentFeignClient).refundPayment(eq(7L), contains("재고 차감 실패"));
        verify(salesService, never()).cancelSales(any(), any(), any());
        verify(orchestrationStateService).markPaymentRefunded(any(PaymentOrchestrationEntity.class));
    }

    @Test
    @DisplayName("결제 완료 이벤트 상품 정보가 잘못되면 재고 호출 없이 결제를 환불하고 주문을 취소한다")
    void handlePaymentCompleted_WhenInventoryItemInvalid_RefundsWithoutInventoryCall() {
        // given
        PaymentOrchestrationService service = service();
        PaymentCompletedEvent event = paymentCompletedEvent(List.of(
                PaymentCompletedEvent.OrderItem.builder()
                        .productId(1999L)
                        .quantity(0)
                        .build()
        ));
        givenStartedOrchestration();

        // when
        service.handlePaymentCompleted(event);

        // then
        verify(inventoryFeignClient, never()).decreaseInventory(any(InventoryChangeDTO.Request.class));
        verify(paymentFeignClient).refundPayment(eq(7L), contains("결제 완료 이벤트 상품 수량은 1 이상"));
        verify(salesService).cancelSales(eq(7L), eq("order-7"), contains("결제 완료 이벤트 상품 수량은 1 이상"));
        verify(salesService, never()).completeSales(any(), any());
    }

    @Test
    @DisplayName("주문 완료 실패 시 차감한 재고를 복구하고 결제를 환불한 뒤 주문을 취소한다")
    void handlePaymentCompleted_WhenCompleteSalesFails_RestoresInventoryAndRefundsPayment() {
        // given
        PaymentOrchestrationService service = service();
        PaymentCompletedEvent event = paymentCompletedEvent();
        givenStartedOrchestration();
        given(inventoryFeignClient.decreaseInventory(any(InventoryChangeDTO.Request.class)))
                .willReturn(InventoryChangeDTO.Response.builder()
                        .success(true)
                        .processedCount(2)
                        .build());
        given(inventoryFeignClient.increaseInventory(any(InventoryChangeDTO.Request.class)))
                .willReturn(InventoryChangeDTO.Response.builder()
                        .success(true)
                        .processedCount(2)
                        .build());
        doThrow(new RuntimeException("주문 DB 장애"))
                .when(salesService).completeSales(7L, "order-7");

        // when
        service.handlePaymentCompleted(event);

        // then
        verify(inventoryFeignClient).decreaseInventory(any(InventoryChangeDTO.Request.class));
        verify(inventoryFeignClient).increaseInventory(any(InventoryChangeDTO.Request.class));
        verify(paymentFeignClient).refundPayment(eq(7L), contains("주문 완료 실패"));
        verify(salesService).cancelSales(eq(7L), eq("order-7"), contains("주문 완료 실패"));
    }

    @Test
    @DisplayName("보상 중 재처리되면 완료된 단계는 건너뛰고 남은 보상을 이어간다")
    void handlePaymentCompleted_WhenCompensating_RestoresRemainingStepsOnly() {
        // given
        PaymentOrchestrationService service = service();
        PaymentCompletedEvent event = paymentCompletedEvent();
        PaymentOrchestrationEntity orchestration = compensatingAfterInventoryDeducted();

        given(orchestrationStateService.getOrCreate("order-7", 7L)).willReturn(orchestration);
        givenStateTransitionsReturnSameEntity();
        given(inventoryFeignClient.increaseInventory(any(InventoryChangeDTO.Request.class)))
                .willReturn(InventoryChangeDTO.Response.builder()
                        .success(true)
                        .processedCount(2)
                        .build());

        // when
        service.handlePaymentCompleted(event);

        // then
        verify(inventoryFeignClient, never()).decreaseInventory(any(InventoryChangeDTO.Request.class));
        verify(inventoryFeignClient).increaseInventory(any(InventoryChangeDTO.Request.class));
        verify(paymentFeignClient).refundPayment(eq(7L), contains("주문 완료 실패"));
        verify(salesService).cancelSales(eq(7L), eq("order-7"), contains("주문 완료 실패"));
    }

    @Test
    @DisplayName("재고 복구 체크포인트 저장 실패 상태에서도 남은 보상은 시도하고 재처리를 유도한다")
    void handlePaymentCompleted_WhenInventoryRestoreCheckpointFailsAndRetried_RestoresInventoryAgain() {
        // given
        PaymentOrchestrationService service = service();
        PaymentCompletedEvent event = paymentCompletedEvent();
        PaymentOrchestrationEntity firstAttempt = compensatingAfterInventoryDeducted();
        PaymentOrchestrationEntity retryAttempt = compensatingAfterInventoryDeducted();

        givenStateTransitionsReturnSameEntity();
        given(orchestrationStateService.getOrCreate("order-7", 7L))
                .willReturn(firstAttempt)
                .willReturn(retryAttempt);
        given(inventoryFeignClient.increaseInventory(any(InventoryChangeDTO.Request.class)))
                .willReturn(InventoryChangeDTO.Response.builder()
                        .success(true)
                        .processedCount(2)
                        .build());
        given(orchestrationStateService.markInventoryRestored(any(PaymentOrchestrationEntity.class)))
                .willThrow(new RuntimeException("checkpoint 저장 실패"));

        // when & then
        assertThatThrownBy(() -> service.handlePaymentCompleted(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("checkpoint 저장 실패");
        assertThatThrownBy(() -> service.handlePaymentCompleted(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("checkpoint 저장 실패");

        ArgumentCaptor<InventoryChangeDTO.Request> requestCaptor =
                ArgumentCaptor.forClass(InventoryChangeDTO.Request.class);
        verify(inventoryFeignClient, times(2)).increaseInventory(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(InventoryChangeDTO.Request::getOperationId)
                .containsOnly("payment-orchestration:7:inventory-restore");
        verify(paymentFeignClient, times(2)).refundPayment(eq(7L), contains("주문 완료 실패"));
        verify(salesService, times(2)).cancelSales(eq(7L), eq("order-7"), contains("주문 완료 실패"));
    }

    @Test
    @DisplayName("재고 차감 요청에는 판매별 차감 멱등성 키를 포함한다")
    void handlePaymentCompleted_SendsInventoryDeductOperationId() {
        // given
        PaymentOrchestrationService service = service();
        PaymentCompletedEvent event = paymentCompletedEvent();
        givenStartedOrchestration();
        given(inventoryFeignClient.decreaseInventory(any(InventoryChangeDTO.Request.class)))
                .willReturn(InventoryChangeDTO.Response.builder()
                        .success(true)
                        .processedCount(2)
                        .build());

        // when
        service.handlePaymentCompleted(event);

        // then
        ArgumentCaptor<InventoryChangeDTO.Request> requestCaptor =
                ArgumentCaptor.forClass(InventoryChangeDTO.Request.class);
        verify(inventoryFeignClient).decreaseInventory(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getOperationId())
                .isEqualTo("payment-orchestration:7:inventory-deduct");
    }

    @Test
    @DisplayName("이미 완료된 오케스트레이션이면 외부 호출을 다시 하지 않는다")
    void handlePaymentCompleted_WhenAlreadyCompleted_SkipsExternalCalls() {
        // given
        PaymentOrchestrationService service = service();
        PaymentCompletedEvent event = paymentCompletedEvent();
        PaymentOrchestrationEntity orchestration =
                PaymentOrchestrationEntity.start("order-7", 7L);
        orchestration.markInventoryDeducted();
        orchestration.markOrderCompleted();

        given(orchestrationStateService.getOrCreate("order-7", 7L)).willReturn(orchestration);

        // when
        service.handlePaymentCompleted(event);

        // then
        verify(inventoryFeignClient, never()).decreaseInventory(any(InventoryChangeDTO.Request.class));
        verify(inventoryFeignClient, never()).increaseInventory(any(InventoryChangeDTO.Request.class));
        verify(paymentFeignClient, never()).refundPayment(any(), any());
        verify(salesService, never()).completeSales(any(), any());
        verify(salesService, never()).cancelSales(any(), any(), any());
    }

    private PaymentOrchestrationService service() {
        return new PaymentOrchestrationService(
                salesService,
                inventoryFeignClient,
                paymentFeignClient,
                orchestrationStateService
        );
    }

    private void givenStartedOrchestration() {
        PaymentOrchestrationEntity orchestration = PaymentOrchestrationEntity.start("order-7", 7L);
        given(orchestrationStateService.getOrCreate("order-7", 7L)).willReturn(orchestration);
        givenStateTransitionsReturnSameEntity();
    }

    private void givenStateTransitionsReturnSameEntity() {
        lenient().when(orchestrationStateService.markInventoryDeducted(any(PaymentOrchestrationEntity.class)))
                .thenAnswer(invocation -> {
                    PaymentOrchestrationEntity orchestration = invocation.getArgument(0);
                    orchestration.markInventoryDeducted();
                    return orchestration;
                });
        lenient().when(orchestrationStateService.markOrderCompleted(any(PaymentOrchestrationEntity.class)))
                .thenAnswer(invocation -> {
                    PaymentOrchestrationEntity orchestration = invocation.getArgument(0);
                    orchestration.markOrderCompleted();
                    return orchestration;
                });
        lenient().when(orchestrationStateService.startCompensation(
                any(PaymentOrchestrationEntity.class),
                any(String.class)
        )).thenAnswer(invocation -> {
            PaymentOrchestrationEntity orchestration = invocation.getArgument(0);
            orchestration.startCompensation(invocation.getArgument(1));
            return orchestration;
        });
        lenient().when(orchestrationStateService.markInventoryRestored(any(PaymentOrchestrationEntity.class)))
                .thenAnswer(invocation -> {
                    PaymentOrchestrationEntity orchestration = invocation.getArgument(0);
                    orchestration.markInventoryRestored();
                    return orchestration;
                });
        lenient().when(orchestrationStateService.markPaymentRefunded(any(PaymentOrchestrationEntity.class)))
                .thenAnswer(invocation -> {
                    PaymentOrchestrationEntity orchestration = invocation.getArgument(0);
                    orchestration.markPaymentRefunded();
                    return orchestration;
                });
        lenient().when(orchestrationStateService.markOrderCancelled(any(PaymentOrchestrationEntity.class)))
                .thenAnswer(invocation -> {
                    PaymentOrchestrationEntity orchestration = invocation.getArgument(0);
                    orchestration.markOrderCancelled();
                    return orchestration;
        });
    }

    private PaymentOrchestrationEntity compensatingAfterInventoryDeducted() {
        PaymentOrchestrationEntity orchestration = PaymentOrchestrationEntity.start("order-7", 7L);
        orchestration.markInventoryDeducted();
        orchestration.startCompensation("주문 완료 실패");
        return orchestration;
    }

    private PaymentCompletedEvent paymentCompletedEvent() {
        return paymentCompletedEvent(List.of(
                PaymentCompletedEvent.OrderItem.builder()
                        .productId(1999L)
                        .productName("상품_1999")
                        .quantity(2)
                        .price(20000)
                        .build(),
                PaymentCompletedEvent.OrderItem.builder()
                        .productId(2000L)
                        .productName("상품_2000")
                        .quantity(1)
                        .price(10000)
                        .build()
        ));
    }

    private PaymentCompletedEvent paymentCompletedEvent(List<PaymentCompletedEvent.OrderItem> items) {
        return PaymentCompletedEvent.builder()
                .eventId("event-7")
                .orderId("order-7")
                .salesId(7L)
                .totalAmount(50000)
                .completedAt(LocalDateTime.now())
                .items(items)
                .build();
    }
}

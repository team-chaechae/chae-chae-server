package com.project.inventoryservice.application.service.sales;

import com.project.inventoryservice.application.response.sales.ResSalesInventoryDTO;
import com.project.inventoryservice.application.service.StockCacheService;
import com.project.inventoryservice.infrastructure.kafka.InventoryEvent;
import com.project.inventoryservice.infrastructure.kafka.InventoryEventProducer;
import com.project.inventoryservice.presentation.request.sales.ReqSalesInventoryDTO;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesInventoryServiceImpl")
class SalesInventoryServiceImplTest {

    @Mock
    private StockCacheService stockCacheService;

    @Mock
    private InventoryEventProducer eventProducer;

    @Mock
    private SalesInventoryIdempotencyService idempotencyService;

    @Test
    @DisplayName("중복 재고 복구 operationId이면 재고를 다시 증가시키지 않는다")
    void increaseInventory_WhenOperationDuplicated_DoesNotIncreaseStockAgain() {
        // given
        SalesInventoryServiceImpl service = service();
        ReqSalesInventoryDTO request = request("payment-orchestration:7:inventory-restore");
        given(idempotencyService.execute(eq("payment-orchestration:7:inventory-restore"), any()))
                .willReturn(ResSalesInventoryDTO.duplicate());

        // when
        ResSalesInventoryDTO response = service.increaseInventory(request);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isDuplicate()).isTrue();
        assertThat(response.getProcessedCount()).isZero();
        verify(stockCacheService, never()).increaseStock(any(), any());
        verify(eventProducer, never()).publish(any(InventoryEvent.class));
    }

    @Test
    @DisplayName("신규 재고 복구 operationId이면 재고를 증가시키고 이벤트를 발행한다")
    void increaseInventory_WhenOperationNew_IncreasesStockAndPublishesEvent() {
        // given
        SalesInventoryServiceImpl service = service();
        ReqSalesInventoryDTO request = request("payment-orchestration:7:inventory-restore");
        given(idempotencyService.execute(eq("payment-orchestration:7:inventory-restore"), any()))
                .willAnswer(invocation -> {
                    Supplier<ResSalesInventoryDTO> command = invocation.getArgument(1);
                    return command.get();
                });
        given(stockCacheService.increaseStock(1999L, 2)).willReturn(12);

        // when
        ResSalesInventoryDTO response = service.increaseInventory(request);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isDuplicate()).isFalse();
        assertThat(response.getProcessedCount()).isEqualTo(1);
        verify(stockCacheService).increaseStock(1999L, 2);
        verify(eventProducer).publish(any(InventoryEvent.class));
    }

    @Test
    @DisplayName("중복 재고 차감 operationId이면 재고를 다시 차감하지 않는다")
    void decreaseInventory_WhenOperationDuplicated_DoesNotDecreaseStockAgain() {
        // given
        SalesInventoryServiceImpl service = service();
        ReqSalesInventoryDTO request = request("payment-orchestration:7:inventory-deduct");
        given(idempotencyService.execute(eq("payment-orchestration:7:inventory-deduct"), any()))
                .willReturn(ResSalesInventoryDTO.duplicate());

        // when
        ResSalesInventoryDTO response = service.decreaseInventory(request);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isDuplicate()).isTrue();
        assertThat(response.getProcessedCount()).isZero();
        verify(stockCacheService, never()).decreaseStock(any(), any());
        verify(eventProducer, never()).publish(any(InventoryEvent.class));
    }

    @Test
    @DisplayName("재고 증가 후 이벤트 발행이 실패하면 증가분을 롤백하고 예외를 던진다")
    void increaseInventory_WhenEventPublishFails_RollsBackIncreasedStock() {
        // given
        SalesInventoryServiceImpl service = service();
        ReqSalesInventoryDTO request = request("payment-orchestration:7:inventory-restore");
        given(idempotencyService.execute(eq("payment-orchestration:7:inventory-restore"), any()))
                .willAnswer(invocation -> {
                    Supplier<ResSalesInventoryDTO> command = invocation.getArgument(1);
                    return command.get();
                });
        given(stockCacheService.increaseStock(1999L, 2)).willReturn(12);
        doThrow(new RuntimeException("Kafka 발행 실패"))
                .when(eventProducer)
                .publish(any(InventoryEvent.class));

        // when & then
        assertThatThrownBy(() -> service.increaseInventory(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kafka 발행 실패");
        verify(stockCacheService).increaseStock(1999L, 2);
        verify(stockCacheService).decreaseStock(1999L, 2);
    }

    @Test
    @DisplayName("재고 차감 후 이벤트 발행이 실패하면 차감분을 롤백하고 예외를 던진다")
    void decreaseInventory_WhenEventPublishFails_RollsBackDecreasedStock() {
        // given
        SalesInventoryServiceImpl service = service();
        ReqSalesInventoryDTO request = request("payment-orchestration:7:inventory-deduct");
        given(idempotencyService.execute(eq("payment-orchestration:7:inventory-deduct"), any()))
                .willAnswer(invocation -> {
                    Supplier<ResSalesInventoryDTO> command = invocation.getArgument(1);
                    return command.get();
                });
        given(stockCacheService.decreaseStock(1999L, 2)).willReturn(8);
        doThrow(new RuntimeException("Kafka 발행 실패"))
                .when(eventProducer)
                .publish(any(InventoryEvent.class));

        // when & then
        assertThatThrownBy(() -> service.decreaseInventory(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kafka 발행 실패");
        verify(stockCacheService).decreaseStock(1999L, 2);
        verify(stockCacheService).increaseStock(1999L, 2);
    }

    @Test
    @DisplayName("다건 재고 증가 중 이벤트 발행이 실패하면 이전 성공분까지 모두 롤백한다")
    void increaseInventory_WhenSecondEventPublishFails_RollsBackAllIncreasedStock() {
        // given
        SalesInventoryServiceImpl service = service();
        ReqSalesInventoryDTO request = request(
                "payment-orchestration:7:inventory-restore",
                List.of(item(1999L, 2), item(2000L, 1))
        );
        given(idempotencyService.execute(eq("payment-orchestration:7:inventory-restore"), any()))
                .willAnswer(invocation -> {
                    Supplier<ResSalesInventoryDTO> command = invocation.getArgument(1);
                    return command.get();
                });
        given(stockCacheService.increaseStock(1999L, 2)).willReturn(12);
        given(stockCacheService.increaseStock(2000L, 1)).willReturn(6);
        doNothing()
                .doThrow(new RuntimeException("Kafka 발행 실패"))
                .when(eventProducer)
                .publish(any(InventoryEvent.class));

        // when & then
        assertThatThrownBy(() -> service.increaseInventory(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kafka 발행 실패");
        verify(stockCacheService).decreaseStock(2000L, 1);
        verify(stockCacheService).decreaseStock(1999L, 2);
    }

    @Test
    @DisplayName("다건 재고 차감 중 이벤트 발행이 실패하면 이전 성공분까지 모두 롤백한다")
    void decreaseInventory_WhenSecondEventPublishFails_RollsBackAllDecreasedStock() {
        // given
        SalesInventoryServiceImpl service = service();
        ReqSalesInventoryDTO request = request(
                "payment-orchestration:7:inventory-deduct",
                List.of(item(1999L, 2), item(2000L, 1))
        );
        given(idempotencyService.execute(eq("payment-orchestration:7:inventory-deduct"), any()))
                .willAnswer(invocation -> {
                    Supplier<ResSalesInventoryDTO> command = invocation.getArgument(1);
                    return command.get();
                });
        given(stockCacheService.decreaseStock(1999L, 2)).willReturn(8);
        given(stockCacheService.decreaseStock(2000L, 1)).willReturn(4);
        doNothing()
                .doThrow(new RuntimeException("Kafka 발행 실패"))
                .when(eventProducer)
                .publish(any(InventoryEvent.class));

        // when & then
        assertThatThrownBy(() -> service.decreaseInventory(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kafka 발행 실패");
        verify(stockCacheService).increaseStock(2000L, 1);
        verify(stockCacheService).increaseStock(1999L, 2);
    }

    private SalesInventoryServiceImpl service() {
        return new SalesInventoryServiceImpl(stockCacheService, eventProducer, idempotencyService);
    }

    private ReqSalesInventoryDTO request(String operationId) {
        return request(operationId, List.of(item(1999L, 2)));
    }

    private ReqSalesInventoryDTO request(String operationId, List<ReqSalesInventoryDTO.SalesInventoryItem> items) {
        return ReqSalesInventoryDTO.builder()
                .operationId(operationId)
                .items(items)
                .build();
    }

    private ReqSalesInventoryDTO.SalesInventoryItem item(Long productId, Integer quantity) {
        return ReqSalesInventoryDTO.SalesInventoryItem.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
    }
}

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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

    private SalesInventoryServiceImpl service() {
        return new SalesInventoryServiceImpl(stockCacheService, eventProducer, idempotencyService);
    }

    private ReqSalesInventoryDTO request(String operationId) {
        return ReqSalesInventoryDTO.builder()
                .operationId(operationId)
                .items(List.of(ReqSalesInventoryDTO.SalesInventoryItem.builder()
                        .productId(1999L)
                        .quantity(2)
                        .build()))
                .build();
    }
}

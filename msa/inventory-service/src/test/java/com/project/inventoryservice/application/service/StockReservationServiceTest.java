package com.project.inventoryservice.application.service;

import com.project.inventoryservice.application.response.ResReleaseStockDTO;
import com.project.inventoryservice.application.response.ResReserveStockDTO;
import com.project.inventoryservice.infrastructure.kafka.InventoryEvent;
import com.project.inventoryservice.infrastructure.kafka.InventoryEventProducer;
import com.project.inventoryservice.infrastructure.repository.JdbcInventoryRepository;
import com.project.inventoryservice.presentation.request.ReqReleaseStockDTO;
import com.project.inventoryservice.presentation.request.ReqReserveStockDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockReservationService")
class StockReservationServiceTest {

    @Mock
    private StockCacheService stockCacheService;

    @Mock
    private InventoryEventProducer eventProducer;

    @Mock
    private JdbcInventoryRepository jdbcInventoryRepository;

    @Test
    @DisplayName("예약 중 이벤트 발행이 실패하면 차감분을 롤백하고 발행된 예약 이벤트를 보상한다")
    void reserveStock_WhenSecondEventPublishFails_RollsBackDeductedStockAndPublishesCompensation() {
        // given
        StockReservationService service = service();
        ReqReserveStockDTO request = reserveRequest();
        given(stockCacheService.decreaseStock(1999L, 2)).willReturn(8);
        given(stockCacheService.decreaseStock(2000L, 1)).willReturn(4);
        given(stockCacheService.increaseStock(2000L, 1)).willReturn(5);
        given(stockCacheService.increaseStock(1999L, 2)).willReturn(10);
        doNothing()
                .doThrow(new RuntimeException("Kafka 발행 실패"))
                .doNothing()
                .when(eventProducer)
                .publish(any(InventoryEvent.class));

        // when
        ResReserveStockDTO response = service.reserveStock(request);

        // then
        assertThat(response.isSuccess()).isFalse();
        verify(stockCacheService).increaseStock(2000L, 1);
        verify(stockCacheService).increaseStock(1999L, 2);

        ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
        verify(eventProducer, times(3)).publish(eventCaptor.capture());
        InventoryEvent compensationEvent = eventCaptor.getAllValues().get(2);
        assertThat(compensationEvent.getProductId()).isEqualTo(1999L);
        assertThat(compensationEvent.getQuantity()).isEqualTo(2);
        assertThat(compensationEvent.getChangeType()).isEqualTo("ORDER_RESTORE");
        assertThat(compensationEvent.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("예약 해제 중 이벤트 발행이 실패하면 복구분을 롤백하고 cancel 마커를 남기지 않는다")
    void releaseStock_WhenSecondEventPublishFails_RollsBackRestoredStockAndSkipsCancelMarker() {
        // given
        StockReservationService service = service();
        ReqReleaseStockDTO request = releaseRequest();
        given(stockCacheService.increaseStock(1999L, 2)).willReturn(12);
        given(stockCacheService.increaseStock(2000L, 1)).willReturn(6);
        given(stockCacheService.decreaseStock(2000L, 1)).willReturn(5);
        given(stockCacheService.decreaseStock(1999L, 2)).willReturn(10);
        doNothing()
                .doThrow(new RuntimeException("Kafka 발행 실패"))
                .doNothing()
                .when(eventProducer)
                .publish(any(InventoryEvent.class));

        // when
        ResReleaseStockDTO response = service.releaseStock(request);

        // then
        assertThat(response.isSuccess()).isFalse();
        verify(stockCacheService).decreaseStock(2000L, 1);
        verify(stockCacheService).decreaseStock(1999L, 2);
        verify(jdbcInventoryRepository, never()).cancelByOrderId(any());

        ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
        verify(eventProducer, times(3)).publish(eventCaptor.capture());
        InventoryEvent compensationEvent = eventCaptor.getAllValues().get(2);
        assertThat(compensationEvent.getProductId()).isEqualTo(1999L);
        assertThat(compensationEvent.getQuantity()).isEqualTo(-2);
        assertThat(compensationEvent.getChangeType()).isEqualTo("ORDER_DECREASE");
        assertThat(compensationEvent.getStatus()).isEqualTo("RESERVED");
    }

    @Test
    @DisplayName("예약 해제 cancel 마커 저장이 실패하면 복구분을 롤백한다")
    void releaseStock_WhenCancelMarkerFails_RollsBackRestoredStock() {
        // given
        StockReservationService service = service();
        ReqReleaseStockDTO request = releaseRequest();
        given(stockCacheService.increaseStock(1999L, 2)).willReturn(12);
        given(stockCacheService.increaseStock(2000L, 1)).willReturn(6);
        given(stockCacheService.decreaseStock(2000L, 1)).willReturn(5);
        given(stockCacheService.decreaseStock(1999L, 2)).willReturn(10);
        doThrow(new RuntimeException("DB 장애"))
                .when(jdbcInventoryRepository)
                .cancelByOrderId("order-7");

        // when
        ResReleaseStockDTO response = service.releaseStock(request);

        // then
        assertThat(response.isSuccess()).isFalse();
        verify(stockCacheService).decreaseStock(2000L, 1);
        verify(stockCacheService).decreaseStock(1999L, 2);
        verify(eventProducer, times(4)).publish(any(InventoryEvent.class));
    }

    private StockReservationService service() {
        return new StockReservationService(stockCacheService, eventProducer, jdbcInventoryRepository);
    }

    private ReqReserveStockDTO reserveRequest() {
        return ReqReserveStockDTO.builder()
                .orderId("order-7")
                .salesId(7L)
                .items(List.of(
                        ReqReserveStockDTO.ReserveItem.builder()
                                .productId(1999L)
                                .quantity(2)
                                .build(),
                        ReqReserveStockDTO.ReserveItem.builder()
                                .productId(2000L)
                                .quantity(1)
                                .build()
                ))
                .build();
    }

    private ReqReleaseStockDTO releaseRequest() {
        return ReqReleaseStockDTO.builder()
                .orderId("order-7")
                .salesId(7L)
                .reason("결제 실패")
                .items(List.of(
                        ReqReleaseStockDTO.ReleaseItem.builder()
                                .productId(1999L)
                                .quantity(2)
                                .build(),
                        ReqReleaseStockDTO.ReleaseItem.builder()
                                .productId(2000L)
                                .quantity(1)
                                .build()
                ))
                .build();
    }
}

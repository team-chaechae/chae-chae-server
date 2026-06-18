package com.project.orderservice.application.service;

import com.project.orderservice.application.global.exception.BadRequestException;
import com.project.orderservice.application.response.ResSalesCreateDTO;
import com.project.orderservice.domain.model.SalesEntity;
import com.project.orderservice.domain.repository.SalesDeliveryStatusRepository;
import com.project.orderservice.domain.repository.SalesRepository;
import com.project.orderservice.infrastructure.client.InventoryFeignClient;
import com.project.orderservice.infrastructure.client.ProductCacheClient;
import com.project.orderservice.infrastructure.client.dto.ProductDTO;
import com.project.orderservice.infrastructure.client.dto.StockReservationDTO;
import com.project.orderservice.presentation.request.ReqCreateSalesDTO;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesServiceImpl 주문 생성 테스트")
class SalesServiceImplCreateTest {

    @Mock
    private SalesRepository salesRepository;

    @Mock
    private ProductCacheClient productCacheClient;

    @Mock
    private InventoryFeignClient inventoryFeignClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SalesDeliveryStatusRepository salesDeliveryStatusRepository;

    private SalesServiceImpl salesService;

    @BeforeEach
    void setUp() {
        salesService = new SalesServiceImpl(
                salesRepository,
                productCacheClient,
                inventoryFeignClient,
                eventPublisher,
                new SimpleMeterRegistry(),
                salesDeliveryStatusRepository
        );
    }

    @Test
    @DisplayName("상품 조회 후 주문을 저장하고 생성 이벤트를 발행한다")
    void createSales_SavesSalesAndPublishesEvent() {
        ReqCreateSalesDTO dto = ReqCreateSalesDTO.builder()
                .userId(1L)
                .deliveryAddress(deliveryAddress())
                .salesItems(List.of(
                        ReqCreateSalesDTO.SalesItem.builder().productId(10L).quantity(2).build(),
                        ReqCreateSalesDTO.SalesItem.builder().productId(20L).quantity(1).build()
                ))
                .build();

        given(productCacheClient.getProductsByIds(List.of(10L, 20L))).willReturn(Map.of(
                10L, ProductDTO.builder().productId(10L).name("사과").price(1000).build(),
                20L, ProductDTO.builder().productId(20L).name("배").price(2000).build()
        ));
        given(salesRepository.save(org.mockito.ArgumentMatchers.any(SalesEntity.class)))
                .willAnswer(invocation -> {
                    SalesEntity sales = invocation.getArgument(0);
                    ReflectionTestUtils.setField(sales, "id", 1L);
                    return sales;
                });
        given(inventoryFeignClient.reserveStock(any(StockReservationDTO.ReserveRequest.class)))
                .willReturn(StockReservationDTO.ReserveResponse.builder()
                        .orderId("order-1")
                        .salesId(1L)
                        .success(true)
                        .build());

        ResSalesCreateDTO result = salesService.createSales(dto);

        assertThat(result.getSalesId()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalPrice()).isEqualTo(4000);

        ArgumentCaptor<SalesEntity> salesCaptor = ArgumentCaptor.forClass(SalesEntity.class);
        verify(salesRepository).save(salesCaptor.capture());
        SalesEntity savedSales = salesCaptor.getValue();
        assertThat(savedSales.getUserId()).isEqualTo(1L);
        assertThat(savedSales.getItems()).hasSize(2);
        assertThat(savedSales.getDeliveryAddress().getRecipientName()).isEqualTo("홍길동");
        assertThat(savedSales.getDeliveryAddress().getRecipientPhone()).isEqualTo("010-1234-5678");
        assertThat(savedSales.getDeliveryAddress().getZipCode()).isEqualTo("12345");
        assertThat(savedSales.getDeliveryAddress().getAddress()).isEqualTo("서울시 강남구");
        assertThat(savedSales.getDeliveryAddress().getAddressDetail()).isEqualTo("101동 1001호");
        assertThat(savedSales.getDeliveryAddress().getDeliveryMemo()).isEqualTo("문 앞");
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(Object.class));
        ArgumentCaptor<StockReservationDTO.ReserveRequest> reserveCaptor =
                ArgumentCaptor.forClass(StockReservationDTO.ReserveRequest.class);
        verify(inventoryFeignClient).reserveStock(reserveCaptor.capture());
        assertThat(reserveCaptor.getValue().getSalesId()).isEqualTo(1L);
        assertThat(reserveCaptor.getValue().getItems()).hasSize(2);
        assertThat(reserveCaptor.getValue().getItems().get(0).getProductId()).isEqualTo(10L);
        assertThat(reserveCaptor.getValue().getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(reserveCaptor.getValue().getItems().get(1).getProductId()).isEqualTo(20L);
        assertThat(reserveCaptor.getValue().getItems().get(1).getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("재고 예약에 실패하면 주문 생성 이벤트를 발행하지 않는다")
    void createSales_WhenStockReservationFails_DoesNotPublishEvent() {
        ReqCreateSalesDTO dto = ReqCreateSalesDTO.builder()
                .userId(1L)
                .deliveryAddress(deliveryAddress())
                .salesItems(List.of(
                        ReqCreateSalesDTO.SalesItem.builder().productId(10L).quantity(2).build()
                ))
                .build();

        given(productCacheClient.getProductsByIds(List.of(10L))).willReturn(Map.of(
                10L, ProductDTO.builder().productId(10L).name("사과").price(1000).build()
        ));
        given(salesRepository.save(org.mockito.ArgumentMatchers.any(SalesEntity.class)))
                .willAnswer(invocation -> {
                    SalesEntity sales = invocation.getArgument(0);
                    ReflectionTestUtils.setField(sales, "id", 1L);
                    return sales;
                });
        given(inventoryFeignClient.reserveStock(any(StockReservationDTO.ReserveRequest.class)))
                .willReturn(StockReservationDTO.ReserveResponse.builder()
                        .orderId("order-1")
                        .salesId(1L)
                        .success(false)
                        .failureReason("상품 10: 재고 부족")
                        .build());

        assertThatThrownBy(() -> salesService.createSales(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("재고 예약 실패");

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("재고 예약 후 주문 트랜잭션이 롤백되면 예약 재고를 해제한다")
    void createSales_WhenTransactionRollsBackAfterReservation_ReleasesReservedStock() {
        ReqCreateSalesDTO dto = ReqCreateSalesDTO.builder()
                .userId(1L)
                .deliveryAddress(deliveryAddress())
                .salesItems(List.of(
                        ReqCreateSalesDTO.SalesItem.builder().productId(10L).quantity(2).build()
                ))
                .build();

        given(productCacheClient.getProductsByIds(List.of(10L))).willReturn(Map.of(
                10L, ProductDTO.builder().productId(10L).name("사과").price(1000).build()
        ));
        given(salesRepository.save(org.mockito.ArgumentMatchers.any(SalesEntity.class)))
                .willAnswer(invocation -> {
                    SalesEntity sales = invocation.getArgument(0);
                    ReflectionTestUtils.setField(sales, "id", 1L);
                    return sales;
                });
        given(inventoryFeignClient.reserveStock(any(StockReservationDTO.ReserveRequest.class)))
                .willReturn(StockReservationDTO.ReserveResponse.builder()
                        .orderId("order-1")
                        .salesId(1L)
                        .success(true)
                        .build());
        given(inventoryFeignClient.releaseStock(any(StockReservationDTO.ReleaseRequest.class)))
                .willReturn(StockReservationDTO.ReleaseResponse.builder()
                        .orderId("order-1")
                        .salesId(1L)
                        .success(true)
                        .build());

        TransactionSynchronizationManager.initSynchronization();
        try {
            salesService.createSales(dto);

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization ->
                            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        ArgumentCaptor<StockReservationDTO.ReleaseRequest> releaseCaptor =
                ArgumentCaptor.forClass(StockReservationDTO.ReleaseRequest.class);
        verify(inventoryFeignClient).releaseStock(releaseCaptor.capture());
        assertThat(releaseCaptor.getValue().getSalesId()).isEqualTo(1L);
        assertThat(releaseCaptor.getValue().getReason()).isEqualTo("주문 트랜잭션 롤백");
        assertThat(releaseCaptor.getValue().getItems()).hasSize(1);
        assertThat(releaseCaptor.getValue().getItems().get(0).getProductId()).isEqualTo(10L);
        assertThat(releaseCaptor.getValue().getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("상품 정보가 없으면 주문을 저장하지 않고 예외를 던진다")
    void createSales_ThrowsWhenProductMissing() {
        ReqCreateSalesDTO dto = ReqCreateSalesDTO.builder()
                .userId(1L)
                .deliveryAddress(deliveryAddress())
                .salesItems(List.of(
                        ReqCreateSalesDTO.SalesItem.builder().productId(10L).quantity(2).build()
                ))
                .build();

        given(productCacheClient.getProductsByIds(List.of(10L))).willReturn(Map.of());

        assertThatThrownBy(() -> salesService.createSales(dto))
                .isInstanceOf(BadRequestException.class);
    }

    private ReqCreateSalesDTO.DeliveryAddress deliveryAddress() {
        return ReqCreateSalesDTO.DeliveryAddress.builder()
                .recipientName("홍길동")
                .recipientPhone("010-1234-5678")
                .zipCode("12345")
                .address("서울시 강남구")
                .addressDetail("101동 1001호")
                .deliveryMemo("문 앞")
                .build();
    }
}

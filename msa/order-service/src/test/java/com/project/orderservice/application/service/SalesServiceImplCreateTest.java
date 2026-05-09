package com.project.orderservice.application.service;

import com.project.orderservice.application.global.exception.BadRequestException;
import com.project.orderservice.application.response.ResSalesCreateDTO;
import com.project.orderservice.domain.model.SalesEntity;
import com.project.orderservice.domain.repository.SalesRepository;
import com.project.orderservice.infrastructure.client.ProductCacheClient;
import com.project.orderservice.infrastructure.client.dto.ProductDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesServiceImpl 주문 생성 테스트")
class SalesServiceImplCreateTest {

    @Mock
    private SalesRepository salesRepository;

    @Mock
    private ProductCacheClient productCacheClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SalesServiceImpl salesService;

    @BeforeEach
    void setUp() {
        salesService = new SalesServiceImpl(
                salesRepository,
                productCacheClient,
                eventPublisher,
                new SimpleMeterRegistry()
        );
    }

    @Test
    @DisplayName("상품 조회 후 주문을 저장하고 생성 이벤트를 발행한다")
    void createSales_SavesSalesAndPublishesEvent() {
        ReqCreateSalesDTO dto = ReqCreateSalesDTO.builder()
                .userId(1L)
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

        ResSalesCreateDTO result = salesService.createSales(dto);

        assertThat(result.getSalesId()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalPrice()).isEqualTo(4000);

        ArgumentCaptor<SalesEntity> salesCaptor = ArgumentCaptor.forClass(SalesEntity.class);
        verify(salesRepository).save(salesCaptor.capture());
        assertThat(salesCaptor.getValue().getItems()).hasSize(2);
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Test
    @DisplayName("상품 정보가 없으면 주문을 저장하지 않고 예외를 던진다")
    void createSales_ThrowsWhenProductMissing() {
        ReqCreateSalesDTO dto = ReqCreateSalesDTO.builder()
                .userId(1L)
                .salesItems(List.of(
                        ReqCreateSalesDTO.SalesItem.builder().productId(10L).quantity(2).build()
                ))
                .build();

        given(productCacheClient.getProductsByIds(List.of(10L))).willReturn(Map.of());

        assertThatThrownBy(() -> salesService.createSales(dto))
                .isInstanceOf(BadRequestException.class);
    }
}

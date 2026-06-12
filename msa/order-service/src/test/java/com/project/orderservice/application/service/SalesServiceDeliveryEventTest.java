package com.project.orderservice.application.service;

import com.project.orderservice.application.event.DeliveryCreateRequestedInternalEvent;
import com.project.orderservice.domain.model.DeliveryAddressSnapshot;
import com.project.orderservice.domain.model.SalesEntity;
import com.project.orderservice.domain.repository.SalesDeliveryStatusRepository;
import com.project.orderservice.domain.repository.SalesRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesServiceImpl 배송 생성 이벤트")
class SalesServiceDeliveryEventTest {

    @Mock
    private SalesRepository salesRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SalesDeliveryStatusRepository salesDeliveryStatusRepository;

    @Test
    @DisplayName("주문 완료 시 배송 생성 요청 이벤트를 발행한다")
    void completeSales_PublishesDeliveryCreateRequestedEvent() {
        SalesEntity sales = SalesEntity.createWithItems(
                "order-7",
                10L,
                deliveryAddress(),
                List.of()
        );
        org.springframework.test.util.ReflectionTestUtils.setField(sales, "id", 7L);
        given(salesRepository.findSalesBySalesIdSimple(7L)).willReturn(sales);

        SalesServiceImpl service = new SalesServiceImpl(
                salesRepository,
                null,
                eventPublisher,
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),
                salesDeliveryStatusRepository
        );

        service.completeSales(7L, "order-7");

        ArgumentCaptor<DeliveryCreateRequestedInternalEvent> eventCaptor =
                ArgumentCaptor.forClass(DeliveryCreateRequestedInternalEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        DeliveryCreateRequestedInternalEvent event = eventCaptor.getValue();
        assertThat(event.getOrderId()).isEqualTo("order-7");
        assertThat(event.getSalesId()).isEqualTo(7L);
        assertThat(event.getUserId()).isEqualTo(10L);
        assertThat(event.getRecipientName()).isEqualTo("홍길동");
        assertThat(event.getMessageKey()).isEqualTo("order-7");
        assertThat(event.getAggregateId()).isEqualTo("7");
    }

    @Test
    @DisplayName("이미 완료된 주문은 배송 생성 요청 이벤트를 다시 발행하지 않는다")
    void completeSales_WhenAlreadyCompleted_DoesNotPublishDeliveryEventAgain() {
        SalesEntity sales = SalesEntity.createWithItems(
                "order-7",
                10L,
                deliveryAddress(),
                List.of()
        );
        sales.complete();
        given(salesRepository.findSalesBySalesIdSimple(7L)).willReturn(sales);

        SalesServiceImpl service = new SalesServiceImpl(
                salesRepository,
                null,
                eventPublisher,
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),
                salesDeliveryStatusRepository
        );

        service.completeSales(7L, "order-7");

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    private DeliveryAddressSnapshot deliveryAddress() {
        return DeliveryAddressSnapshot.create(
                "홍길동",
                "010-1234-5678",
                "12345",
                "서울시 강남구",
                "101동 1001호",
                "문 앞"
        );
    }
}

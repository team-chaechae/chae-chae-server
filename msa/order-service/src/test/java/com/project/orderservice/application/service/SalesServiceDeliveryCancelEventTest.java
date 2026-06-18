package com.project.orderservice.application.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.project.orderservice.application.event.DeliveryCancelRequestedInternalEvent;
import com.project.orderservice.domain.model.DeliveryAddressSnapshot;
import com.project.orderservice.domain.model.SalesEntity;
import com.project.orderservice.domain.repository.SalesDeliveryStatusRepository;
import com.project.orderservice.domain.repository.SalesRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesServiceImpl 배송 취소 보상 이벤트")
class SalesServiceDeliveryCancelEventTest {

    @Mock
    private SalesRepository salesRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SalesDeliveryStatusRepository salesDeliveryStatusRepository;

    @Test
    @DisplayName("완료된 주문 취소 시 배송 취소 요청 이벤트를 발행한다")
    void cancelCompletedSales_PublishesDeliveryCancelRequestedEvent() {
        SalesEntity sales = sales("order-7");
        ReflectionTestUtils.setField(sales, "id", 7L);
        sales.complete();
        given(salesRepository.findSalesBySalesIdSimple(7L)).willReturn(sales);
        SalesServiceImpl service = service();

        service.cancelSales(7L, "order-7", "환불 완료");

        ArgumentCaptor<DeliveryCancelRequestedInternalEvent> captor =
                ArgumentCaptor.forClass(DeliveryCancelRequestedInternalEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getSalesId()).isEqualTo(7L);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getOrderId()).isEqualTo("order-7");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getReason()).isEqualTo("환불 완료");
    }

    @Test
    @DisplayName("완료 전 주문 취소 시 배송 취소 요청 이벤트를 발행하지 않는다")
    void cancelPendingSales_DoesNotPublishDeliveryCancelRequestedEvent() {
        SalesEntity sales = sales("order-7");
        ReflectionTestUtils.setField(sales, "id", 7L);
        given(salesRepository.findSalesBySalesIdSimple(7L)).willReturn(sales);
        SalesServiceImpl service = service();

        service.cancelSales(7L, "order-7", "주문 취소");

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    private SalesServiceImpl service() {
        return new SalesServiceImpl(
                salesRepository,
                null,
                null,
                eventPublisher,
                new SimpleMeterRegistry(),
                salesDeliveryStatusRepository
        );
    }

    private SalesEntity sales(String orderId) {
        return SalesEntity.createWithItems(
                orderId,
                10L,
                DeliveryAddressSnapshot.create(
                        "홍길동",
                        "010-1234-5678",
                        "12345",
                        "서울시 강남구",
                        "101동 1001호",
                        "문 앞"
                ),
                List.of()
        );
    }
}

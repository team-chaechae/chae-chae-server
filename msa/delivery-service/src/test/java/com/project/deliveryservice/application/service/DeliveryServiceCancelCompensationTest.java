package com.project.deliveryservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.deliveryservice.application.event.DeliveryStatusChangedInternalEvent;
import com.project.deliveryservice.domain.model.DeliveryCancellationRequestEntity;
import com.project.deliveryservice.domain.model.DeliveryEntity;
import com.project.deliveryservice.domain.repository.DeliveryCancellationRequestRepository;
import com.project.deliveryservice.domain.repository.DeliveryRepository;
import com.project.deliveryservice.presentation.request.ReqCreateDeliveryDTO;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceCancelCompensationTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryCancellationRequestRepository cancellationRequestRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<DeliveryStatusChangedInternalEvent> eventCaptor;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-11T06:20:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void cancelDeliveryBySalesId_WhenDeliveryExists_CancelsAndPublishesStatusEvent() {
        DeliveryEntity delivery = delivery();
        when(cancellationRequestRepository.findBySalesId(7L)).thenReturn(Optional.empty());
        when(deliveryRepository.findBySalesIdForUpdate(7L)).thenReturn(Optional.of(delivery));
        DeliveryServiceImpl service = service();

        service.cancelDeliveryBySalesId(7L, "order-7", "환불 완료");

        assertThat(delivery.getStatus().name()).isEqualTo("CANCELLED");
        verify(cancellationRequestRepository).save(any(DeliveryCancellationRequestEntity.class));
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getStatus().name()).isEqualTo("CANCELLED");
        assertThat(eventCaptor.getValue().getSalesId()).isEqualTo(7L);
    }

    @Test
    void cancelDeliveryBySalesId_WhenDeliveryMissing_RecordsCancellationMarkerOnly() {
        when(cancellationRequestRepository.findBySalesId(7L)).thenReturn(Optional.empty());
        when(deliveryRepository.findBySalesIdForUpdate(7L)).thenReturn(Optional.empty());
        DeliveryServiceImpl service = service();

        service.cancelDeliveryBySalesId(7L, "order-7", "환불 완료");

        verify(cancellationRequestRepository).save(any(DeliveryCancellationRequestEntity.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void createDeliveryFromEventIfAbsent_WhenCancellationMarkerExists_SkipsCreate() {
        when(cancellationRequestRepository.existsBySalesId(7L)).thenReturn(true);
        DeliveryServiceImpl service = service();

        service.createDeliveryFromEventIfAbsent(createRequest());

        verify(deliveryRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private DeliveryServiceImpl service() {
        return new DeliveryServiceImpl(
                deliveryRepository,
                cancellationRequestRepository,
                clock,
                eventPublisher
        );
    }

    private DeliveryEntity delivery() {
        DeliveryEntity delivery = DeliveryEntity.create(
                7L,
                10L,
                "홍길동",
                "010-1234-5678",
                "12345",
                "서울시 강남구",
                "101동 1001호",
                "문 앞"
        );
        ReflectionTestUtils.setField(delivery, "id", 12L);
        return delivery;
    }

    private ReqCreateDeliveryDTO createRequest() {
        return ReqCreateDeliveryDTO.builder()
                .salesId(7L)
                .userId(10L)
                .recipientName("홍길동")
                .recipientPhone("010-1234-5678")
                .zipCode("12345")
                .address("서울시 강남구")
                .addressDetail("101동 1001호")
                .deliveryMemo("문 앞")
                .build();
    }
}

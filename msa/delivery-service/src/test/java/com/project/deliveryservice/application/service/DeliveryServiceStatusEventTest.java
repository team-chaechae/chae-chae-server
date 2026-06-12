package com.project.deliveryservice.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.deliveryservice.application.event.DeliveryStatusChangedInternalEvent;
import com.project.deliveryservice.domain.model.DeliveryEntity;
import com.project.deliveryservice.domain.repository.DeliveryCancellationRequestRepository;
import com.project.deliveryservice.domain.repository.DeliveryRepository;
import com.project.deliveryservice.presentation.request.ReqAssignTrackingDTO;
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
class DeliveryServiceStatusEventTest {

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
    void createDeliveryPublishesReadyStatusEvent() {
        DeliveryServiceImpl service = service();
        DeliveryEntity delivery = delivery();
        when(deliveryRepository.existsBySalesId(7L)).thenReturn(false);
        when(deliveryRepository.save(any(DeliveryEntity.class))).thenReturn(delivery);

        service.createDelivery(createRequest());

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        DeliveryStatusChangedInternalEvent event = eventCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(event.getDeliveryId()).isEqualTo(12L);
        org.assertj.core.api.Assertions.assertThat(event.getSalesId()).isEqualTo(7L);
        org.assertj.core.api.Assertions.assertThat(event.getStatus().name()).isEqualTo("READY");
    }

    @Test
    void duplicateCreateDeliveryIfAbsentDoesNotPublishStatusEvent() {
        DeliveryServiceImpl service = service();
        when(deliveryRepository.findBySalesId(7L)).thenReturn(Optional.of(delivery()));

        service.createDeliveryIfAbsent(createRequest());

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shipDeliveryPublishesInTransitStatusEvent() {
        DeliveryServiceImpl service = service();
        DeliveryEntity delivery = delivery();
        when(deliveryRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(delivery));

        service.assignTrackingNumber(12L, ReqAssignTrackingDTO.builder().trackingNumber("TRACK-1").build());
        service.shipDelivery(12L);

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        DeliveryStatusChangedInternalEvent event = eventCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(event.getStatus().name()).isEqualTo("IN_TRANSIT");
        org.assertj.core.api.Assertions.assertThat(event.getTrackingNumber()).isEqualTo("TRACK-1");
    }

    private DeliveryEntity delivery() {
        DeliveryEntity delivery = DeliveryEntity.create(
                7L,
                3L,
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

    private DeliveryServiceImpl service() {
        return new DeliveryServiceImpl(
                deliveryRepository,
                cancellationRequestRepository,
                clock,
                eventPublisher
        );
    }

    private ReqCreateDeliveryDTO createRequest() {
        return ReqCreateDeliveryDTO.builder()
                .salesId(7L)
                .userId(3L)
                .recipientName("홍길동")
                .recipientPhone("010-1234-5678")
                .zipCode("12345")
                .address("서울시 강남구")
                .addressDetail("101동 1001호")
                .deliveryMemo("문 앞")
                .build();
    }
}

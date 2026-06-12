package com.project.deliveryservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.deliveryservice.application.global.dto.ResDeliveryDTO;
import com.project.deliveryservice.application.global.exception.BadRequestException;
import com.project.deliveryservice.domain.model.constraint.DeliveryStatus;
import com.project.deliveryservice.domain.repository.DeliveryRepository;
import com.project.deliveryservice.presentation.request.ReqAssignTrackingDTO;
import com.project.deliveryservice.presentation.request.ReqCreateDeliveryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DeliveryServiceImplTest {

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @BeforeEach
    void setUp() {
        deliveryRepository.deleteAll();
    }

    @Test
    void createDeliveryCreatesReadyDelivery() {
        ResDeliveryDTO delivery = createDelivery(1L);

        assertThat(delivery.id()).isNotNull();
        assertThat(delivery.salesId()).isEqualTo(1L);
        assertThat(delivery.status()).isEqualTo(DeliveryStatus.READY);
    }

    @Test
    void createDeliveryRejectsDuplicateSalesId() {
        createDelivery(1L);

        assertThatThrownBy(() -> createDelivery(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이미 생성된 배송 정보입니다.");
    }

    @Test
    void createDeliveryIfAbsentReturnsExistingDeliveryForDuplicateSalesId() {
        ResDeliveryDTO created = createDelivery(1L);

        ResDeliveryDTO duplicated = deliveryService.createDeliveryIfAbsent(createDeliveryRequest(1L));

        assertThat(duplicated.id()).isEqualTo(created.id());
        assertThat(duplicated.salesId()).isEqualTo(1L);
        assertThat(deliveryRepository.count()).isEqualTo(1L);
    }

    @Test
    void shipDeliveryRequiresTrackingNumber() {
        ResDeliveryDTO delivery = createDelivery(1L);

        assertThatThrownBy(() -> deliveryService.shipDelivery(delivery.id()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("운송장 등록 후 출고할 수 있습니다.");
    }

    @Test
    void deliveryCanMoveFromReadyToInTransitAndDelivered() {
        ResDeliveryDTO delivery = createDelivery(1L);
        deliveryService.assignTrackingNumber(
                delivery.id(),
                ReqAssignTrackingDTO.builder()
                        .trackingNumber("TRACK-1")
                        .build()
        );

        ResDeliveryDTO shipped = deliveryService.shipDelivery(delivery.id());
        ResDeliveryDTO completed = deliveryService.completeDelivery(delivery.id());

        assertThat(shipped.status()).isEqualTo(DeliveryStatus.IN_TRANSIT);
        assertThat(shipped.shippedAt()).isNotNull();
        assertThat(completed.status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(completed.deliveredAt()).isNotNull();
    }

    @Test
    void deliveredDeliveryCannotBeCancelled() {
        ResDeliveryDTO delivery = createDelivery(1L);
        deliveryService.assignTrackingNumber(
                delivery.id(),
                ReqAssignTrackingDTO.builder()
                        .trackingNumber("TRACK-1")
                        .build()
        );
        deliveryService.shipDelivery(delivery.id());
        deliveryService.completeDelivery(delivery.id());

        assertThatThrownBy(() -> deliveryService.cancelDelivery(delivery.id()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("배송 완료 건은 취소할 수 없습니다.");
    }

    private ResDeliveryDTO createDelivery(Long salesId) {
        return deliveryService.createDelivery(createDeliveryRequest(salesId));
    }

    private ReqCreateDeliveryDTO createDeliveryRequest(Long salesId) {
        return ReqCreateDeliveryDTO.builder()
                .salesId(salesId)
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

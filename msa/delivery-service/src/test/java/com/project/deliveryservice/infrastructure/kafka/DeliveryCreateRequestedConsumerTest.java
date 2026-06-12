package com.project.deliveryservice.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.deliveryservice.application.service.DeliveryService;
import com.project.deliveryservice.presentation.request.ReqCreateDeliveryDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryCreateRequestedConsumer")
class DeliveryCreateRequestedConsumerTest {

    @Mock
    private DeliveryService deliveryService;

    @Mock
    private Acknowledgment acknowledgment;

    @Test
    @DisplayName("배송 생성 요청 이벤트를 수신하면 취소 마커를 고려해 멱등 생성 후 ack한다")
    void handleDeliveryCreateRequested_CreatesDeliveryAndAcknowledges() throws Exception {
        DeliveryCreateRequestedConsumer consumer = new DeliveryCreateRequestedConsumer(
                deliveryService,
                new ObjectMapper().findAndRegisterModules()
        );

        consumer.handleDeliveryCreateRequested(payload(), acknowledgment);

        ArgumentCaptor<ReqCreateDeliveryDTO> requestCaptor = ArgumentCaptor.forClass(ReqCreateDeliveryDTO.class);
        verify(deliveryService).createDeliveryFromEventIfAbsent(requestCaptor.capture());
        ReqCreateDeliveryDTO request = requestCaptor.getValue();
        assertThat(request.getSalesId()).isEqualTo(7L);
        assertThat(request.getUserId()).isEqualTo(10L);
        assertThat(request.getRecipientName()).isEqualTo("홍길동");
        assertThat(request.getRecipientPhone()).isEqualTo("010-1234-5678");
        assertThat(request.getZipCode()).isEqualTo("12345");
        assertThat(request.getAddress()).isEqualTo("서울시 강남구");
        assertThat(request.getAddressDetail()).isEqualTo("101동 1001호");
        assertThat(request.getDeliveryMemo()).isEqualTo("문 앞");
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("배송 생성 실패 시 ack하지 않고 예외를 전파한다")
    void handleDeliveryCreateRequested_WhenCreateFails_DoesNotAcknowledge() {
        DeliveryCreateRequestedConsumer consumer = new DeliveryCreateRequestedConsumer(
                deliveryService,
                new ObjectMapper().findAndRegisterModules()
        );
        org.mockito.BDDMockito.willThrow(new RuntimeException("DB 장애"))
                .given(deliveryService).createDeliveryFromEventIfAbsent(any(ReqCreateDeliveryDTO.class));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> consumer.handleDeliveryCreateRequested(payload(), acknowledgment)
                )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB 장애");
        verify(acknowledgment, never()).acknowledge();
    }

    private String payload() {
        return """
                {
                  "eventId": "event-7",
                  "orderId": "order-7",
                  "salesId": 7,
                  "userId": 10,
                  "recipientName": "홍길동",
                  "recipientPhone": "010-1234-5678",
                  "zipCode": "12345",
                  "address": "서울시 강남구",
                  "addressDetail": "101동 1001호",
                  "deliveryMemo": "문 앞"
                }
                """;
    }
}

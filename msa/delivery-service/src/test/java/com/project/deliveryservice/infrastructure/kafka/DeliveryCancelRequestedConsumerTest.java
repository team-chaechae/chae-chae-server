package com.project.deliveryservice.infrastructure.kafka;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.deliveryservice.application.service.DeliveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryCancelRequestedConsumer")
class DeliveryCancelRequestedConsumerTest {

    @Mock
    private DeliveryService deliveryService;

    @Mock
    private Acknowledgment acknowledgment;

    @Test
    @DisplayName("배송 취소 요청 이벤트를 수신하면 salesId 기준 취소 후 ack한다")
    void handleDeliveryCancelRequested_CancelsDeliveryAndAcknowledges() throws Exception {
        DeliveryCancelRequestedConsumer consumer = new DeliveryCancelRequestedConsumer(
                deliveryService,
                new ObjectMapper().findAndRegisterModules()
        );

        consumer.handleDeliveryCancelRequested(payload(), acknowledgment);

        verify(deliveryService).cancelDeliveryBySalesId(7L, "order-7", "환불 완료");
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("배송 취소 실패 시 ack하지 않고 예외를 전파한다")
    void handleDeliveryCancelRequested_WhenCancelFails_DoesNotAcknowledge() {
        DeliveryCancelRequestedConsumer consumer = new DeliveryCancelRequestedConsumer(
                deliveryService,
                new ObjectMapper().findAndRegisterModules()
        );
        org.mockito.BDDMockito.willThrow(new RuntimeException("DB 장애"))
                .given(deliveryService).cancelDeliveryBySalesId(7L, "order-7", "환불 완료");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> consumer.handleDeliveryCancelRequested(payload(), acknowledgment)
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
                  "reason": "환불 완료"
                }
                """;
    }
}

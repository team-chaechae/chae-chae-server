package com.project.orderservice.application.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.orderservice.application.event.DeliveryCancelRequestedInternalEvent;
import com.project.orderservice.application.event.DeliveryCreateRequestedInternalEvent;
import com.project.orderservice.application.event.OrderCreatedInternalEvent;
import com.project.orderservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.orderservice.domain.repository.OutboxRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventSendService")
class OrderEventSendServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private OutboxRepository outboxRepository;

    @Test
    @DisplayName("Kafka 발행 성공 시 Outbox를 SEND_SUCCESS로 변경한다")
    void sendOrderCreated_Success_MarksOutboxSuccess() {
        OrderEventSendService service = new OrderEventSendService(
                kafkaTemplate,
                outboxRepository,
                new ObjectMapper().findAndRegisterModules()
        );
        OrderCreatedInternalEvent event = event();
        given(kafkaTemplate.send(eq("order-created"), eq("order-1"), anyString()))
                .willReturn(CompletableFuture.completedFuture(null));
        given(outboxRepository.updateStatusSuccessByAggregateIdAndEventType(
                eq("10"),
                eq("ORDER_CREATED"),
                eq(OutboxStatus.INIT),
                eq(OutboxStatus.SEND_SUCCESS),
                any()
        )).willReturn(1);

        service.sendOrderCreated(event);

        verify(outboxRepository).updateStatusSuccessByAggregateIdAndEventType(
                eq("10"),
                eq("ORDER_CREATED"),
                eq(OutboxStatus.INIT),
                eq(OutboxStatus.SEND_SUCCESS),
                any()
        );
        verify(outboxRepository, never()).updateStatusFailByAggregateIdAndEventType(
                anyString(), anyString(), any(), any(), anyString(), any()
        );
    }

    @Test
    @DisplayName("Kafka 발행 실패 시 Outbox를 SEND_FAIL로 변경한다")
    void sendOrderCreated_Failure_MarksOutboxFail() {
        OrderEventSendService service = new OrderEventSendService(
                kafkaTemplate,
                outboxRepository,
                new ObjectMapper().findAndRegisterModules()
        );
        OrderCreatedInternalEvent event = event();
        given(kafkaTemplate.send(eq("order-created"), eq("order-1"), anyString()))
                .willReturn(CompletableFuture.failedFuture(new TimeoutException("kafka timeout")));
        given(outboxRepository.updateStatusFailByAggregateIdAndEventType(
                eq("10"),
                eq("ORDER_CREATED"),
                eq(OutboxStatus.INIT),
                eq(OutboxStatus.SEND_FAIL),
                anyString(),
                any()
        )).willReturn(1);

        service.sendOrderCreated(event);

        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxRepository).updateStatusFailByAggregateIdAndEventType(
                eq("10"),
                eq("ORDER_CREATED"),
                eq(OutboxStatus.INIT),
                eq(OutboxStatus.SEND_FAIL),
                errorCaptor.capture(),
                any()
        );
        assertThat(errorCaptor.getValue()).contains("kafka timeout");
    }

    @Test
    @DisplayName("Kafka 발행 성공 후 Outbox 성공 업데이트 대상이 없으면 예외를 던진다")
    void sendOrderCreated_SuccessButOutboxUpdateAffectedZeroRows_ThrowsException() {
        OrderEventSendService service = new OrderEventSendService(
                kafkaTemplate,
                outboxRepository,
                new ObjectMapper().findAndRegisterModules()
        );
        OrderCreatedInternalEvent event = event();
        given(kafkaTemplate.send(eq("order-created"), eq("order-1"), anyString()))
                .willReturn(CompletableFuture.completedFuture(null));
        given(outboxRepository.updateStatusSuccessByAggregateIdAndEventType(
                eq("10"),
                eq("ORDER_CREATED"),
                eq(OutboxStatus.INIT),
                eq(OutboxStatus.SEND_SUCCESS),
                any()
        )).willReturn(0);

        assertThatThrownBy(() -> service.sendOrderCreated(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Outbox 성공 상태 업데이트 실패");
    }

    @Test
    @DisplayName("배송 생성 요청 Kafka 발행 성공 시 Outbox를 SEND_SUCCESS로 변경한다")
    void sendDeliveryCreateRequested_Success_MarksOutboxSuccess() {
        OrderEventSendService service = new OrderEventSendService(
                kafkaTemplate,
                outboxRepository,
                new ObjectMapper().findAndRegisterModules()
        );
        DeliveryCreateRequestedInternalEvent event = deliveryEvent();
        given(kafkaTemplate.send(eq("delivery-create-requested"), eq("order-1"), anyString()))
                .willReturn(CompletableFuture.completedFuture(null));
        given(outboxRepository.updateStatusSuccessByAggregateIdAndEventType(
                eq("10"),
                eq("DELIVERY_CREATE_REQUESTED"),
                eq(OutboxStatus.INIT),
                eq(OutboxStatus.SEND_SUCCESS),
                any()
        )).willReturn(1);

        service.sendDeliveryCreateRequested(event);

        verify(outboxRepository).updateStatusSuccessByAggregateIdAndEventType(
                eq("10"),
                eq("DELIVERY_CREATE_REQUESTED"),
                eq(OutboxStatus.INIT),
                eq(OutboxStatus.SEND_SUCCESS),
                any()
        );
    }

    @Test
    @DisplayName("배송 취소 요청 Kafka 발행 성공 시 Outbox를 SEND_SUCCESS로 변경한다")
    void sendDeliveryCancelRequested_Success_MarksOutboxSuccess() {
        OrderEventSendService service = new OrderEventSendService(
                kafkaTemplate,
                outboxRepository,
                new ObjectMapper().findAndRegisterModules()
        );
        DeliveryCancelRequestedInternalEvent event = DeliveryCancelRequestedInternalEvent.of(
                "order-1",
                10L,
                "환불 완료"
        );
        given(kafkaTemplate.send(eq("delivery-cancel-requested"), eq("order-1"), anyString()))
                .willReturn(CompletableFuture.completedFuture(null));
        given(outboxRepository.updateStatusSuccessByAggregateIdAndEventType(
                eq("10"),
                eq("DELIVERY_CANCEL_REQUESTED"),
                eq(OutboxStatus.INIT),
                eq(OutboxStatus.SEND_SUCCESS),
                any()
        )).willReturn(1);

        service.sendDeliveryCancelRequested(event);

        verify(outboxRepository).updateStatusSuccessByAggregateIdAndEventType(
                eq("10"),
                eq("DELIVERY_CANCEL_REQUESTED"),
                eq(OutboxStatus.INIT),
                eq(OutboxStatus.SEND_SUCCESS),
                any()
        );
    }

    private OrderCreatedInternalEvent event() {
        return OrderCreatedInternalEvent.of(
                "order-1",
                10L,
                List.of(OrderCreatedInternalEvent.OrderItem.builder()
                        .productId(1L)
                        .productName("product")
                        .quantity(1)
                        .price(1000)
                        .build()),
                1000
        );
    }

    private DeliveryCreateRequestedInternalEvent deliveryEvent() {
        return DeliveryCreateRequestedInternalEvent.of(
                "order-1",
                10L,
                1L,
                "홍길동",
                "010-1234-5678",
                "12345",
                "서울시 강남구",
                "101동 1001호",
                "문 앞"
        );
    }
}

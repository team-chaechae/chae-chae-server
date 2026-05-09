package com.project.common.dlq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.dlq.alert.DlqAlertService;
import com.project.common.dlq.config.DlqProperties;
import com.project.common.dlq.domain.DlqMessage;
import com.project.common.dlq.producer.DlqMessageProducer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TechnicalDlqReplayConsumer")
class TechnicalDlqReplayConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("원본 토픽 재발행 실패 시 retryCount를 증가시킨 DLQ 메시지를 다시 발행하고 현재 메시지를 ack한다")
    void handleDlqMessage_replayFails_requeuesWithIncrementedRetryCount() throws Exception {
        DlqMessageProducer producer = mock(DlqMessageProducer.class);
        DlqAlertService alertService = mock(DlqAlertService.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        DlqProperties properties = new DlqProperties();
        TechnicalDlqReplayConsumer consumer = newConsumer(producer, alertService, properties);
        DlqMessage message = technicalMessage(0);
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "order-created.dlq",
                0,
                1L,
                "order-1",
                objectMapper.writeValueAsString(message)
        );

        when(producer.replayToOriginalSync(any(DlqMessage.class))).thenReturn(false);

        consumer.handleDlqMessage(record, acknowledgment);

        ArgumentCaptor<DlqMessage> captor = ArgumentCaptor.forClass(DlqMessage.class);
        verify(producer).sendToDlq(captor.capture());
        assertThat(captor.getValue().getRetryCount()).isEqualTo(1);
        verify(acknowledgment).acknowledge();
        verify(alertService, never()).sendTechnicalReplayFailedAlert(any(), any(Integer.class));
    }

    @Test
    @DisplayName("증가된 DLQ 메시지 재발행도 실패하면 현재 메시지를 ack하지 않는다")
    void handleDlqMessage_requeueFails_doesNotAckCurrentMessage() throws Exception {
        DlqMessageProducer producer = mock(DlqMessageProducer.class);
        DlqAlertService alertService = mock(DlqAlertService.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        DlqProperties properties = new DlqProperties();
        TechnicalDlqReplayConsumer consumer = newConsumer(producer, alertService, properties);
        DlqMessage message = technicalMessage(0);
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "order-created.dlq",
                0,
                1L,
                "order-1",
                objectMapper.writeValueAsString(message)
        );

        when(producer.replayToOriginalSync(any(DlqMessage.class))).thenReturn(false);
        org.mockito.Mockito.doThrow(new IllegalStateException("DLQ publish failed"))
                .when(producer).sendToDlq(any(DlqMessage.class));

        consumer.handleDlqMessage(record, acknowledgment);

        verify(acknowledgment, never()).acknowledge();
    }

    private TechnicalDlqReplayConsumer newConsumer(
            DlqMessageProducer producer,
            DlqAlertService alertService,
            DlqProperties properties
    ) {
        TechnicalDlqReplayConsumer consumer = new TechnicalDlqReplayConsumer(
                producer,
                alertService,
                properties,
                objectMapper
        );
        ReflectionTestUtils.setField(consumer, "serviceName", "payment-service");
        return consumer;
    }

    private DlqMessage technicalMessage(int retryCount) {
        return DlqMessage.builder()
                .originalTopic("order-created")
                .originalKey("order-1")
                .originalPayload("{\"orderId\":\"order-1\"}")
                .serviceName("payment-service")
                .exceptionCategory(DlqMessage.ExceptionCategory.TECHNICAL)
                .retryCount(retryCount)
                .build();
    }
}

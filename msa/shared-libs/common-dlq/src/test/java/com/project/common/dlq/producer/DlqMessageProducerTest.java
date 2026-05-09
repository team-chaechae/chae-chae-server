package com.project.common.dlq.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.dlq.config.DlqProperties;
import com.project.common.dlq.domain.DlqMessage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("DlqMessageProducer")
class DlqMessageProducerTest {

    @Test
    @DisplayName("DLQ 토픽 발행 실패 시 예외를 전파한다")
    void sendToDlq_publishFails_propagatesException() {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        DlqProperties properties = new DlqProperties();
        DlqMessageProducer producer = new DlqMessageProducer(
                kafkaTemplate,
                new ObjectMapper(),
                properties
        );
        DlqMessage message = DlqMessage.builder()
                .originalTopic("order-created")
                .originalKey("order-1")
                .originalPayload("{\"orderId\":\"order-1\"}")
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("kafka timeout")));

        assertThatThrownBy(() -> producer.sendToDlq(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DLQ 발행 실패");
    }
}

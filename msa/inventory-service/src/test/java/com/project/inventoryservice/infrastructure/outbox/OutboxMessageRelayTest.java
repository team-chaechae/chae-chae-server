package com.project.inventoryservice.infrastructure.outbox;

import com.project.inventoryservice.domain.model.OutboxEntity;
import com.project.inventoryservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.inventoryservice.domain.repository.OutboxRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("Inventory OutboxMessageRelay")
class OutboxMessageRelayTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("최종 재시도 실패 후 DLQ 전송이 성공하면 DLQ_SENT로 닫는다")
    void relayFailedMessages_WhenFinalRetryFailsAndDlqSucceeds_MarksDlqSent() {
        // given
        OutboxMessageRelay relay = relay();
        OutboxEntity outbox = failedOutboxWithRetryCount(2);
        given(outboxRepository.findMessagesForRetry(
                eq(OutboxStatus.SEND_SUCCESS),
                eq(List.of(OutboxStatus.INIT, OutboxStatus.SEND_FAIL)),
                any(),
                eq(3),
                eq(100)
        )).willReturn(List.of(outbox));
        given(kafkaTemplate.send("inventory-events", "product-1999", "{}"))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("broker 장애")));
        given(kafkaTemplate.send("inventory-events.dlq", "product-1999", "{}"))
                .willReturn(CompletableFuture.completedFuture(null));

        // when
        relay.relayFailedMessages();

        // then
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.DLQ_SENT);
        assertThat(outbox.getErrorMessage()).contains("broker 장애");
    }

    @Test
    @DisplayName("최종 재시도 실패 후 DLQ 전송도 실패하면 다음 relay에서 재시도 가능하게 남긴다")
    void relayFailedMessages_WhenFinalRetryAndDlqFail_KeepsRetryableFailure() {
        // given
        OutboxMessageRelay relay = relay();
        OutboxEntity outbox = failedOutboxWithRetryCount(2);
        given(outboxRepository.findMessagesForRetry(
                eq(OutboxStatus.SEND_SUCCESS),
                eq(List.of(OutboxStatus.INIT, OutboxStatus.SEND_FAIL)),
                any(),
                eq(3),
                eq(100)
        )).willReturn(List.of(outbox));
        given(kafkaTemplate.send("inventory-events", "product-1999", "{}"))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("broker 장애")));
        given(kafkaTemplate.send("inventory-events.dlq", "product-1999", "{}"))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("dlq 장애")));

        // when
        relay.relayFailedMessages();

        // then
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.SEND_FAIL);
        assertThat(outbox.getRetryCount()).isEqualTo(2);
        assertThat(outbox.getErrorMessage()).contains("DLQ publish failed");
    }

    private OutboxMessageRelay relay() {
        OutboxMessageRelay relay = new OutboxMessageRelay(outboxRepository, kafkaTemplate);
        ReflectionTestUtils.setField(relay, "thresholdMinutes", 10);
        ReflectionTestUtils.setField(relay, "batchSize", 100);
        ReflectionTestUtils.setField(relay, "maxRetries", 3);
        ReflectionTestUtils.setField(relay, "sendTimeoutSeconds", 5L);
        ReflectionTestUtils.setField(relay, "dlqTopicSuffix", ".dlq");
        return relay;
    }

    private OutboxEntity failedOutboxWithRetryCount(int retryCount) {
        OutboxEntity outbox = OutboxEntity.create(
                "INVENTORY",
                "1999",
                "INVENTORY_CHANGED",
                "{}",
                "inventory-events",
                "product-1999"
        );
        for (int i = 0; i < retryCount; i++) {
            outbox.markAsSendFail("previous failure");
        }
        return outbox;
    }
}

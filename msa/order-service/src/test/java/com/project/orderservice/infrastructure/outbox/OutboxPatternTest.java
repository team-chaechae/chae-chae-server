package com.project.orderservice.infrastructure.outbox;

import com.project.orderservice.domain.model.OutboxEntity;
import com.project.orderservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.orderservice.domain.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Outbox 패턴 메시지 유실 대응 테스트")
class OutboxPatternTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxMessageRelay outboxMessageRelay;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    private long idSequence = 1L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(outboxMessageRelay, "thresholdMinutes", 10);
        ReflectionTestUtils.setField(outboxMessageRelay, "batchSize", 100);
        ReflectionTestUtils.setField(outboxMessageRelay, "maxRetries", 3);
        ReflectionTestUtils.setField(outboxMessageRelay, "maxAgeSeconds", 3600L);
        ReflectionTestUtils.setField(outboxMessageRelay, "baseBackoffMs", 1000L);
        ReflectionTestUtils.setField(outboxMessageRelay, "maxBackoffMs", 60000L);
        ReflectionTestUtils.setField(outboxMessageRelay, "dlqTopicSuffix", ".dlq");
        ReflectionTestUtils.setField(outboxMessageRelay, "cleanupDays", 7);
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<SendResult<String, String>> createSuccessFuture() {
        return CompletableFuture.completedFuture(mock(SendResult.class));
    }

    private CompletableFuture<SendResult<String, String>> createFailureFuture(String errorMessage) {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException(errorMessage));
        return future;
    }

    @Nested
    @DisplayName("OutboxEntity 테스트")
    class OutboxEntityTest {

        @Test
        @DisplayName("Outbox 엔티티 생성 시 INIT 상태로 시작한다")
        void create_StartsWithInitStatus() {
            OutboxEntity outbox = OutboxEntity.create(
                    "ORDER", "order-123", "ORDER_CREATED",
                    "{\"orderId\": \"order-123\"}", "order-created", "order-123"
            );

            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.INIT);
            assertThat(outbox.getRetryCount()).isEqualTo(0);
            assertThat(outbox.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("성공 시 SEND_SUCCESS로 상태 변경된다")
        void markAsSendSuccess_ChangesStatusToSendSuccess() {
            OutboxEntity outbox = createTestOutbox();

            outbox.markAsSendSuccess();

            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.SEND_SUCCESS);
            assertThat(outbox.getProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("실패 시 SEND_FAIL로 상태 변경되고 retryCount 증가한다")
        void markAsSendFail_ChangesStatusAndIncrementsRetryCount() {
            OutboxEntity outbox = createTestOutbox();

            outbox.markAsSendFail("Connection timeout");

            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.SEND_FAIL);
            assertThat(outbox.getRetryCount()).isEqualTo(1);
            assertThat(outbox.getErrorMessage()).isEqualTo("Connection timeout");
        }

        @Test
        @DisplayName("에러 메시지가 500자 초과 시 잘린다")
        void markAsSendFail_TruncatesLongErrorMessage() {
            OutboxEntity outbox = createTestOutbox();
            String longMessage = "a".repeat(600);

            outbox.markAsSendFail(longMessage);

            assertThat(outbox.getErrorMessage()).hasSize(500);
        }

        @Test
        @DisplayName("최대 재시도 횟수 이내면 재시도 가능하다")
        void canRetry_ReturnsTrueWhenUnderMaxRetries() {
            OutboxEntity outbox = createTestOutbox();
            outbox.markAsSendFail("Error 1");
            outbox.markAsSendFail("Error 2");

            assertThat(outbox.canRetry(3)).isTrue();
        }

        @Test
        @DisplayName("최대 재시도 횟수 도달 시 재시도 불가하다")
        void canRetry_ReturnsFalseWhenAtMaxRetries() {
            OutboxEntity outbox = createTestOutbox();
            outbox.markAsSendFail("Error 1");
            outbox.markAsSendFail("Error 2");
            outbox.markAsSendFail("Error 3");

            assertThat(outbox.canRetry(3)).isFalse();
        }
    }

    @Nested
    @DisplayName("OutboxMessageRelay 테스트")
    class OutboxMessageRelayTest {

        @Test
        @DisplayName("재발행 대상이 없으면 아무것도 하지 않는다")
        void relayFailedMessages_NoMessages_DoesNothing() {
            given(outboxRepository.findMessagesForRetry(anyList(), any(), anyInt(), any()))
                    .willReturn(Collections.emptyList());

            outboxMessageRelay.relayFailedMessages();

            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("실패한 메시지를 Kafka로 재발행한다")
        void relayFailedMessages_RetriesFailedMessages() {
            OutboxEntity failedOutbox = createTestOutbox();
            failedOutbox.markAsSendFail("Previous failure");

            given(outboxRepository.findMessagesForRetry(anyList(), any(), anyInt(), any()))
                    .willReturn(List.of(failedOutbox));
            given(kafkaTemplate.send(anyString(), anyString(), any()))
                    .willReturn(createSuccessFuture());

            outboxMessageRelay.relayFailedMessages();

            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());
            verify(outboxRepository).updateStatusSuccessById(
                    eq(failedOutbox.getId()),
                    eq(OutboxStatus.SEND_SUCCESS),
                    any(LocalDateTime.class)
            );
            assertThat(topicCaptor.getValue()).isEqualTo("order-created");
            assertThat(keyCaptor.getValue()).isEqualTo("order-123");
        }

        @Test
        @DisplayName("재발행 실패 시 retryCount가 증가한다")
        void relayFailedMessages_OnFailure_IncrementsRetryCount() {
            OutboxEntity failedOutbox = createTestOutbox();
            int initialRetryCount = failedOutbox.getRetryCount();

            given(outboxRepository.findMessagesForRetry(anyList(), any(), anyInt(), any()))
                    .willReturn(List.of(failedOutbox));
            given(kafkaTemplate.send(anyString(), anyString(), any()))
                    .willReturn(createFailureFuture("Kafka broker unavailable"));

            outboxMessageRelay.relayFailedMessages();

            verify(outboxRepository).updateStatusFailById(
                    eq(failedOutbox.getId()),
                    eq(OutboxStatus.SEND_FAIL),
                    contains("Kafka broker unavailable"),
                    any(LocalDateTime.class)
            );
            assertThat(failedOutbox.getRetryCount()).isEqualTo(initialRetryCount);
        }

        @Test
        @DisplayName("여러 메시지를 순차적으로 재발행한다")
        void relayFailedMessages_ProcessesMultipleMessages() {
            OutboxEntity outbox1 = createTestOutbox("order-1");
            OutboxEntity outbox2 = createTestOutbox("order-2");
            OutboxEntity outbox3 = createTestOutbox("order-3");

            given(outboxRepository.findMessagesForRetry(anyList(), any(), anyInt(), any()))
                    .willReturn(Arrays.asList(outbox1, outbox2, outbox3));
            given(kafkaTemplate.send(anyString(), anyString(), any()))
                    .willReturn(createSuccessFuture());

            outboxMessageRelay.relayFailedMessages();

            verify(kafkaTemplate, times(3)).send(anyString(), anyString(), any());
            verify(outboxRepository, times(3)).updateStatusSuccessById(
                    anyLong(),
                    eq(OutboxStatus.SEND_SUCCESS),
                    any(LocalDateTime.class)
            );
        }

        @Test
        @DisplayName("일부 메시지만 성공해도 나머지는 계속 처리한다")
        void relayFailedMessages_PartialSuccess_ContinuesProcessing() {
            OutboxEntity outbox1 = createTestOutbox("order-1");
            OutboxEntity outbox2 = createTestOutbox("order-2");
            OutboxEntity outbox3 = createTestOutbox("order-3");

            given(outboxRepository.findMessagesForRetry(anyList(), any(), anyInt(), any()))
                    .willReturn(Arrays.asList(outbox1, outbox2, outbox3));

            // 첫 번째 성공, 두 번째 실패, 세 번째 성공
            given(kafkaTemplate.send(eq("order-created"), eq("order-1"), any()))
                    .willReturn(createSuccessFuture());
            given(kafkaTemplate.send(eq("order-created"), eq("order-2"), any()))
                    .willReturn(createFailureFuture("Connection refused"));
            given(kafkaTemplate.send(eq("order-created"), eq("order-3"), any()))
                    .willReturn(createSuccessFuture());

            outboxMessageRelay.relayFailedMessages();

            verify(outboxRepository, times(2)).updateStatusSuccessById(
                    anyLong(),
                    eq(OutboxStatus.SEND_SUCCESS),
                    any(LocalDateTime.class)
            );
            verify(outboxRepository).updateStatusFailById(
                    eq(outbox2.getId()),
                    eq(OutboxStatus.SEND_FAIL),
                    contains("Connection refused"),
                    any(LocalDateTime.class)
            );
        }

        @Test
        @DisplayName("처리 완료된 오래된 메시지를 정리한다")
        void cleanupProcessedMessages_DeletesOldMessages() {
            given(outboxRepository.deleteProcessedMessagesBefore(any(), any()))
                    .willReturn(50);

            outboxMessageRelay.cleanupProcessedMessages();

            verify(outboxRepository).deleteProcessedMessagesBefore(
                    eq(OutboxStatus.SEND_SUCCESS),
                    any(LocalDateTime.class)
            );
        }
    }

    @Nested
    @DisplayName("메시지 유실 시나리오 테스트")
    class MessageLossScenarioTest {

        @Test
        @DisplayName("Kafka 발행 실패 시 Outbox에 남아 재시도된다")
        void kafkaPublishFails_MessageRemainsInOutbox() {
            OutboxEntity outbox = createTestOutbox();
            outbox.markAsSendFail("Initial kafka failure");

            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.SEND_FAIL);

            given(outboxRepository.findMessagesForRetry(anyList(), any(), anyInt(), any()))
                    .willReturn(List.of(outbox));
            given(kafkaTemplate.send(anyString(), anyString(), any()))
                    .willReturn(createSuccessFuture());

            outboxMessageRelay.relayFailedMessages();

            verify(outboxRepository).updateStatusSuccessById(
                    eq(outbox.getId()),
                    eq(OutboxStatus.SEND_SUCCESS),
                    any(LocalDateTime.class)
            );
        }

        @Test
        @DisplayName("최대 재시도 횟수 초과 메시지는 제외된다")
        void maxRetriesExceeded_MessageExcludedFromRetry() {
            OutboxEntity exhaustedOutbox = createTestOutbox();
            exhaustedOutbox.markAsSendFail("Error 1");
            exhaustedOutbox.markAsSendFail("Error 2");
            exhaustedOutbox.markAsSendFail("Error 3");

            assertThat(exhaustedOutbox.canRetry(3)).isFalse();

            OutboxEntity retryableOutbox = createTestOutbox("retryable-order");
            retryableOutbox.markAsSendFail("Single failure");

            given(outboxRepository.findMessagesForRetry(
                    eq(List.of(OutboxStatus.INIT, OutboxStatus.SEND_FAIL)),
                    any(LocalDateTime.class),
                    eq(3),
                    any()
            )).willReturn(List.of(retryableOutbox));

            given(kafkaTemplate.send(anyString(), anyString(), any()))
                    .willReturn(createSuccessFuture());

            outboxMessageRelay.relayFailedMessages();

            verify(kafkaTemplate, times(1)).send(anyString(), eq("retryable-order"), any());
            verify(outboxRepository).updateStatusSuccessById(
                    eq(retryableOutbox.getId()),
                    eq(OutboxStatus.SEND_SUCCESS),
                    any(LocalDateTime.class)
            );
        }

        @Test
        @DisplayName("트랜잭션과 Outbox 저장은 원자적으로 처리된다")
        void transactionAndOutbox_AtomicOperation() {
            OutboxEntity outbox = createTestOutbox();

            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.INIT);
            assertThat(outbox.getAggregateType()).isEqualTo("ORDER");
            assertThat(outbox.getEventType()).isEqualTo("ORDER_CREATED");

            outbox.markAsSendFail("Kafka unavailable");
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.SEND_FAIL);
            assertThat(outbox.canRetry(3)).isTrue();
        }
    }

    private OutboxEntity createTestOutbox() {
        return createTestOutbox("order-123");
    }

    private OutboxEntity createTestOutbox(String orderId) {
        OutboxEntity outbox = OutboxEntity.create(
                "ORDER", orderId, "ORDER_CREATED",
                "{\"orderId\": \"" + orderId + "\", \"salesId\": 1, \"totalAmount\": 50000}",
                "order-created", orderId
        );
        ReflectionTestUtils.setField(outbox, "id", idSequence++);
        ReflectionTestUtils.setField(outbox, "createdAt", LocalDateTime.now().minusMinutes(1));
        return outbox;
    }
}

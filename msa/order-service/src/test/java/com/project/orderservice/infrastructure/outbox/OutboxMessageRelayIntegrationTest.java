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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * OutboxMessageRelay 테스트
 *
 * Kafka 발행 실패 시 Outbox 패턴을 통한 메시지 재발행 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxMessageRelay 테스트")
class OutboxMessageRelayIntegrationTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> stringKafkaTemplate;

    @InjectMocks
    private OutboxMessageRelay outboxMessageRelay;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<String> payloadCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(outboxMessageRelay, "thresholdMinutes", 10);
        ReflectionTestUtils.setField(outboxMessageRelay, "batchSize", 100);
        ReflectionTestUtils.setField(outboxMessageRelay, "maxRetries", 3);
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
    @DisplayName("메시지 재발행 테스트")
    class RelayFailedMessagesTest {

        @Test
        @DisplayName("INIT 상태의 메시지를 Kafka로 발행한다")
        void relaysInitStatusMessages() {
            // given
            OutboxEntity outbox = createTestOutbox("order-1");

            given(outboxRepository.findMessagesForRetry(any(), any(), anyInt(), anyInt()))
                    .willReturn(List.of(outbox));
            given(stringKafkaTemplate.send(anyString(), anyString(), anyString()))
                    .willReturn(createSuccessFuture());

            // when
            outboxMessageRelay.relayFailedMessages();

            // then
            verify(stringKafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());
            assertThat(topicCaptor.getValue()).isEqualTo("order-created");
            assertThat(keyCaptor.getValue()).isEqualTo("order-1");
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.SEND_SUCCESS);
        }

        @Test
        @DisplayName("SEND_FAIL 상태의 메시지를 재발행한다")
        void relaysSendFailStatusMessages() {
            // given
            OutboxEntity outbox = createTestOutbox("order-1");
            outbox.markAsSendFail("Previous failure");

            given(outboxRepository.findMessagesForRetry(any(), any(), anyInt(), anyInt()))
                    .willReturn(List.of(outbox));
            given(stringKafkaTemplate.send(anyString(), anyString(), anyString()))
                    .willReturn(createSuccessFuture());

            // when
            outboxMessageRelay.relayFailedMessages();

            // then
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.SEND_SUCCESS);
        }

        @Test
        @DisplayName("여러 메시지를 순차적으로 처리한다")
        void relaysMultipleMessagesSequentially() {
            // given
            OutboxEntity outbox1 = createTestOutbox("order-1");
            OutboxEntity outbox2 = createTestOutbox("order-2");
            OutboxEntity outbox3 = createTestOutbox("order-3");

            given(outboxRepository.findMessagesForRetry(any(), any(), anyInt(), anyInt()))
                    .willReturn(Arrays.asList(outbox1, outbox2, outbox3));
            given(stringKafkaTemplate.send(anyString(), anyString(), anyString()))
                    .willReturn(createSuccessFuture());

            // when
            outboxMessageRelay.relayFailedMessages();

            // then
            verify(stringKafkaTemplate, times(3)).send(anyString(), anyString(), anyString());
            assertThat(outbox1.getStatus()).isEqualTo(OutboxStatus.SEND_SUCCESS);
            assertThat(outbox2.getStatus()).isEqualTo(OutboxStatus.SEND_SUCCESS);
            assertThat(outbox3.getStatus()).isEqualTo(OutboxStatus.SEND_SUCCESS);
        }

        @Test
        @DisplayName("재발행 대상이 없으면 아무것도 하지 않는다")
        void noMessages_DoesNothing() {
            // given
            given(outboxRepository.findMessagesForRetry(any(), any(), anyInt(), anyInt()))
                    .willReturn(Collections.emptyList());

            // when
            outboxMessageRelay.relayFailedMessages();

            // then
            verify(stringKafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("재발행 실패 시 SEND_FAIL로 상태 변경되고 retryCount 증가")
        void relayFailure_UpdatesStatusAndRetryCount() {
            // given
            OutboxEntity outbox = createTestOutbox("order-1");

            given(outboxRepository.findMessagesForRetry(any(), any(), anyInt(), anyInt()))
                    .willReturn(List.of(outbox));
            given(stringKafkaTemplate.send(anyString(), anyString(), anyString()))
                    .willReturn(createFailureFuture("Kafka broker unavailable"));

            // when
            outboxMessageRelay.relayFailedMessages();

            // then
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.SEND_FAIL);
            assertThat(outbox.getRetryCount()).isEqualTo(1);
            assertThat(outbox.getErrorMessage()).contains("Kafka broker unavailable");
        }

        @Test
        @DisplayName("일부 메시지만 성공해도 나머지는 계속 처리한다")
        void partialSuccess_ContinuesProcessing() {
            // given
            OutboxEntity outbox1 = createTestOutbox("order-1");
            OutboxEntity outbox2 = createTestOutbox("order-2");
            OutboxEntity outbox3 = createTestOutbox("order-3");

            given(outboxRepository.findMessagesForRetry(any(), any(), anyInt(), anyInt()))
                    .willReturn(Arrays.asList(outbox1, outbox2, outbox3));

            // 첫 번째 성공, 두 번째 실패, 세 번째 성공
            given(stringKafkaTemplate.send(eq("order-created"), eq("order-1"), anyString()))
                    .willReturn(createSuccessFuture());
            given(stringKafkaTemplate.send(eq("order-created"), eq("order-2"), anyString()))
                    .willReturn(createFailureFuture("Connection refused"));
            given(stringKafkaTemplate.send(eq("order-created"), eq("order-3"), anyString()))
                    .willReturn(createSuccessFuture());

            // when
            outboxMessageRelay.relayFailedMessages();

            // then
            assertThat(outbox1.getStatus()).isEqualTo(OutboxStatus.SEND_SUCCESS);
            assertThat(outbox2.getStatus()).isEqualTo(OutboxStatus.SEND_FAIL);
            assertThat(outbox3.getStatus()).isEqualTo(OutboxStatus.SEND_SUCCESS);
        }
    }

    @Nested
    @DisplayName("메시지 정리 테스트")
    class CleanupProcessedMessagesTest {

        @Test
        @DisplayName("처리 완료된 오래된 메시지를 삭제한다")
        void deletesOldProcessedMessages() {
            // given
            given(outboxRepository.deleteProcessedMessagesBefore(any(), any()))
                    .willReturn(50);

            // when
            outboxMessageRelay.cleanupProcessedMessages();

            // then
            verify(outboxRepository).deleteProcessedMessagesBefore(
                    eq(OutboxStatus.SEND_SUCCESS),
                    any(LocalDateTime.class)
            );
        }
    }

    @Nested
    @DisplayName("Kafka 메시지 내용 검증")
    class KafkaMessageContentTest {

        @Test
        @DisplayName("올바른 토픽, 키, 페이로드로 메시지가 발행된다")
        void publishesWithCorrectContent() {
            // given
            String orderId = "test-order-123";
            String payload = "{\"orderId\": \"" + orderId + "\", \"totalAmount\": 50000}";

            OutboxEntity outbox = OutboxEntity.create(
                    "ORDER",
                    "1",
                    "ORDER_CREATED",
                    payload,
                    "order-created",
                    orderId
            );

            given(outboxRepository.findMessagesForRetry(any(), any(), anyInt(), anyInt()))
                    .willReturn(List.of(outbox));
            given(stringKafkaTemplate.send(anyString(), anyString(), anyString()))
                    .willReturn(createSuccessFuture());

            // when
            outboxMessageRelay.relayFailedMessages();

            // then
            verify(stringKafkaTemplate).send(
                    topicCaptor.capture(),
                    keyCaptor.capture(),
                    payloadCaptor.capture()
            );

            assertThat(topicCaptor.getValue()).isEqualTo("order-created");
            assertThat(keyCaptor.getValue()).isEqualTo(orderId);
            assertThat(payloadCaptor.getValue()).isEqualTo(payload);
        }
    }

    @Nested
    @DisplayName("메시지 유실 방지 시나리오 테스트")
    class MessageLossPreventionTest {

        @Test
        @DisplayName("Kafka 발행 타임아웃 시 메시지가 유실되지 않는다")
        void kafkaTimeout_MessageNotLost() {
            // given
            OutboxEntity outbox = createTestOutbox("order-timeout");

            given(outboxRepository.findMessagesForRetry(any(), any(), anyInt(), anyInt()))
                    .willReturn(List.of(outbox));

            // 타임아웃 시뮬레이션
            CompletableFuture<SendResult<String, String>> timeoutFuture = new CompletableFuture<>();
            timeoutFuture.completeExceptionally(new java.util.concurrent.TimeoutException("Request timeout"));
            given(stringKafkaTemplate.send(anyString(), anyString(), anyString()))
                    .willReturn(timeoutFuture);

            // when
            outboxMessageRelay.relayFailedMessages();

            // then - 메시지가 SEND_FAIL 상태로 유지되어 다음 릴레이에서 재시도됨
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.SEND_FAIL);
            assertThat(outbox.canRetry(3)).isTrue();  // 아직 재시도 가능
        }

        @Test
        @DisplayName("3회 실패 후에는 더 이상 재시도하지 않는다")
        void exhaustedRetries_NoMoreAttempts() {
            // given
            OutboxEntity exhausted = createTestOutbox("order-exhausted");
            exhausted.markAsSendFail("Error 1");
            exhausted.markAsSendFail("Error 2");
            exhausted.markAsSendFail("Error 3");

            // findMessagesForRetry에서 이미 필터링됨 (retryCount < maxRetries 조건)
            given(outboxRepository.findMessagesForRetry(
                    eq(OutboxStatus.SEND_SUCCESS),
                    any(LocalDateTime.class),
                    eq(3),  // maxRetries
                    eq(100)
            )).willReturn(Collections.emptyList());  // exhausted 메시지는 조회되지 않음

            // when
            outboxMessageRelay.relayFailedMessages();

            // then
            verify(stringKafkaTemplate, never()).send(anyString(), anyString(), anyString());
            assertThat(exhausted.canRetry(3)).isFalse();
        }
    }

    private OutboxEntity createTestOutbox(String aggregateId) {
        return OutboxEntity.create(
                "ORDER",
                aggregateId,
                "ORDER_CREATED",
                "{\"orderId\": \"" + aggregateId + "\", \"salesId\": 1, \"totalAmount\": 50000}",
                "order-created",
                aggregateId
        );
    }
}

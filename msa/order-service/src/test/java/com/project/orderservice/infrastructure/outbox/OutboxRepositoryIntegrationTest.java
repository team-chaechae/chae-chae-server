package com.project.orderservice.infrastructure.outbox;

import com.project.orderservice.domain.model.OutboxEntity;
import com.project.orderservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.orderservice.domain.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("OutboxRepository 통합 테스트")
class OutboxRepositoryIntegrationTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Nested
    @DisplayName("findMessagesForRetry 테스트")
    class FindMessagesForRetryTest {

        @Test
        @DisplayName("INIT 상태이고 threshold 이전에 생성된 메시지를 조회한다")
        void findInitStatusMessages() {
            // given
            OutboxEntity oldInitMessage = createAndSaveOutbox("order-1", OutboxStatus.INIT);
            OutboxEntity newInitMessage = createAndSaveOutbox("order-2", OutboxStatus.INIT);

            // when - threshold를 미래로 설정하면 모든 메시지가 대상이 됨
            List<OutboxEntity> result = outboxRepository.findMessagesForRetry(
                    List.of(OutboxStatus.INIT, OutboxStatus.SEND_FAIL),
                    LocalDateTime.now().plusMinutes(10),
                    3,
                    PageRequest.of(0, 100)
            );

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("SEND_FAIL 상태 메시지도 조회한다")
        void findSendFailStatusMessages() {
            // given
            OutboxEntity failedMessage = createAndSaveOutbox("order-1", OutboxStatus.INIT);
            failedMessage.markAsSendFail("Connection timeout");
            outboxRepository.save(failedMessage);

            // when
            List<OutboxEntity> result = outboxRepository.findMessagesForRetry(
                    List.of(OutboxStatus.INIT, OutboxStatus.SEND_FAIL),
                    LocalDateTime.now().plusMinutes(10),
                    3,
                    PageRequest.of(0, 100)
            );

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(OutboxStatus.SEND_FAIL);
        }

        @Test
        @DisplayName("SEND_SUCCESS 상태 메시지는 제외한다")
        void excludeSendSuccessMessages() {
            // given
            OutboxEntity successMessage = createAndSaveOutbox("order-1", OutboxStatus.INIT);
            successMessage.markAsSendSuccess();
            outboxRepository.save(successMessage);

            OutboxEntity pendingMessage = createAndSaveOutbox("order-2", OutboxStatus.INIT);

            // when
            List<OutboxEntity> result = outboxRepository.findMessagesForRetry(
                    List.of(OutboxStatus.INIT, OutboxStatus.SEND_FAIL),
                    LocalDateTime.now().plusMinutes(10),
                    3,
                    PageRequest.of(0, 100)
            );

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAggregateId()).isEqualTo("order-2");
        }

        @Test
        @DisplayName("최대 재시도 횟수를 초과한 메시지는 제외한다")
        void excludeExhaustedRetryMessages() {
            // given
            OutboxEntity exhaustedMessage = createAndSaveOutbox("order-1", OutboxStatus.INIT);
            exhaustedMessage.markAsSendFail("Error 1");
            exhaustedMessage.markAsSendFail("Error 2");
            exhaustedMessage.markAsSendFail("Error 3");
            outboxRepository.save(exhaustedMessage);

            OutboxEntity retryableMessage = createAndSaveOutbox("order-2", OutboxStatus.INIT);
            retryableMessage.markAsSendFail("Single failure");
            outboxRepository.save(retryableMessage);

            // when - maxRetries = 3
            List<OutboxEntity> result = outboxRepository.findMessagesForRetry(
                    List.of(OutboxStatus.INIT, OutboxStatus.SEND_FAIL),
                    LocalDateTime.now().plusMinutes(10),
                    3,
                    PageRequest.of(0, 100)
            );

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAggregateId()).isEqualTo("order-2");
        }

        @Test
        @DisplayName("배치 사이즈만큼만 조회한다")
        void respectBatchSize() {
            // given
            for (int i = 0; i < 10; i++) {
                createAndSaveOutbox("order-" + i, OutboxStatus.INIT);
            }

            // when
            List<OutboxEntity> result = outboxRepository.findMessagesForRetry(
                    List.of(OutboxStatus.INIT, OutboxStatus.SEND_FAIL),
                    LocalDateTime.now().plusMinutes(10),
                    3,
                    PageRequest.of(0, 5)
            );

            // then
            assertThat(result).hasSize(5);
        }

        @Test
        @DisplayName("생성 시간 순으로 정렬된다")
        void orderedByCreatedAt() {
            // given
            OutboxEntity first = createAndSaveOutbox("order-first", OutboxStatus.INIT);
            OutboxEntity second = createAndSaveOutbox("order-second", OutboxStatus.INIT);
            OutboxEntity third = createAndSaveOutbox("order-third", OutboxStatus.INIT);

            // when
            List<OutboxEntity> result = outboxRepository.findMessagesForRetry(
                    List.of(OutboxStatus.INIT, OutboxStatus.SEND_FAIL),
                    LocalDateTime.now().plusMinutes(10),
                    3,
                    PageRequest.of(0, 100)
            );

            // then
            assertThat(result).hasSize(3);
            // 먼저 생성된 순서대로 조회
            assertThat(result.get(0).getAggregateId()).isEqualTo("order-first");
        }
    }

    @Nested
    @DisplayName("findByAggregateIdAndEventTypeAndStatus 테스트")
    class FindByAggregateIdAndEventTypeAndStatusTest {

        @Test
        @DisplayName("aggregateId, eventType, status로 정확히 조회한다")
        void findExactMatch() {
            // given
            createAndSaveOutbox("order-1", OutboxStatus.INIT);

            OutboxEntity successMessage = createAndSaveOutbox("order-1", OutboxStatus.INIT);
            successMessage.markAsSendSuccess();
            outboxRepository.save(successMessage);

            // when
            List<OutboxEntity> initMessages = outboxRepository.findByAggregateIdAndEventTypeAndStatus(
                    "order-1", "ORDER_CREATED", OutboxStatus.INIT
            );

            List<OutboxEntity> successMessages = outboxRepository.findByAggregateIdAndEventTypeAndStatus(
                    "order-1", "ORDER_CREATED", OutboxStatus.SEND_SUCCESS
            );

            // then
            assertThat(initMessages).hasSize(1);
            assertThat(successMessages).hasSize(1);
        }
    }

    @Nested
    @DisplayName("deleteProcessedMessagesBefore 테스트")
    class DeleteProcessedMessagesBeforeTest {

        @Test
        @DisplayName("지정된 날짜 이전에 처리 완료된 메시지를 삭제한다")
        void deleteOldProcessedMessages() {
            // given
            OutboxEntity oldSuccess = createAndSaveOutbox("order-old", OutboxStatus.INIT);
            oldSuccess.markAsSendSuccess();
            outboxRepository.save(oldSuccess);

            OutboxEntity recentSuccess = createAndSaveOutbox("order-recent", OutboxStatus.INIT);
            recentSuccess.markAsSendSuccess();
            outboxRepository.save(recentSuccess);

            OutboxEntity pendingMessage = createAndSaveOutbox("order-pending", OutboxStatus.INIT);

            // when - 미래 시간 기준으로 삭제하면 모든 성공 메시지 삭제됨
            int deleted = outboxRepository.deleteProcessedMessagesBefore(
                    OutboxStatus.SEND_SUCCESS,
                    LocalDateTime.now().plusDays(1)
            );

            // then
            assertThat(deleted).isEqualTo(2);
            assertThat(outboxRepository.findAll()).hasSize(1);
            assertThat(outboxRepository.findAll().get(0).getAggregateId()).isEqualTo("order-pending");
        }

        @Test
        @DisplayName("SEND_SUCCESS가 아닌 메시지는 삭제하지 않는다")
        void doNotDeleteNonSuccessMessages() {
            // given
            OutboxEntity failedMessage = createAndSaveOutbox("order-1", OutboxStatus.INIT);
            failedMessage.markAsSendFail("Error");
            outboxRepository.save(failedMessage);

            OutboxEntity initMessage = createAndSaveOutbox("order-2", OutboxStatus.INIT);

            // when
            int deleted = outboxRepository.deleteProcessedMessagesBefore(
                    OutboxStatus.SEND_SUCCESS,
                    LocalDateTime.now().plusDays(1)
            );

            // then
            assertThat(deleted).isEqualTo(0);
            assertThat(outboxRepository.findAll()).hasSize(2);
        }
    }

    private OutboxEntity createAndSaveOutbox(String aggregateId, OutboxStatus status) {
        OutboxEntity outbox = OutboxEntity.create(
                "ORDER",
                aggregateId,
                "ORDER_CREATED",
                "{\"orderId\": \"" + aggregateId + "\"}",
                "order-created",
                aggregateId
        );
        return outboxRepository.save(outbox);
    }
}

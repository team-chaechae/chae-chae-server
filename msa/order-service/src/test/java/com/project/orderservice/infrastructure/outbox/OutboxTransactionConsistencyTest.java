package com.project.orderservice.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.dlq.domain.DlqRecordRepository;
import com.project.orderservice.domain.model.OutboxEntity;
import com.project.orderservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.orderservice.domain.model.SalesEntity;
import com.project.orderservice.domain.model.SalesItemEntity;
import com.project.orderservice.domain.repository.OutboxRepository;
import com.project.orderservice.domain.repository.SalesRepository;
import com.project.orderservice.infrastructure.config.RedisConfig;
import com.project.orderservice.infrastructure.kafka.dto.OrderCreatedEvent;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = {
                OutboxTransactionConsistencyTest.TestApplication.class,
                OutboxTransactionConsistencyTest.TestConfig.class
        },
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@ActiveProfiles("test")
@DisplayName("Outbox 트랜잭션 일관성 테스트")
class OutboxTransactionConsistencyTest {

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    private TransactionTemplate transactionTemplate;

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(
            basePackages = "com.project.orderservice",
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = RedisConfig.class
            )
    )
    static class TestApplication {
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        RedissonClient redissonClient() {
            return Mockito.mock(RedissonClient.class);
        }

        @Bean
        KafkaTemplate<String, String> dlqKafkaTemplate() {
            return Mockito.mock(KafkaTemplate.class);
        }

        @Bean
        DlqRecordRepository dlqRecordRepository() {
            return Mockito.mock(DlqRecordRepository.class);
        }
    }

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        outboxRepository.deleteAll();
        // 트랜잭션 내에서 SalesEntity 삭제
        transactionTemplate.execute(status -> {
            entityManager.createQuery("DELETE FROM SalesItemEntity").executeUpdate();
            entityManager.createQuery("DELETE FROM SalesEntity").executeUpdate();
            return null;
        });
    }

    @Nested
    @DisplayName("트랜잭션 원자성 테스트")
    class TransactionAtomicityTest {

        @Test
        @DisplayName("주문과 Outbox가 같은 트랜잭션에서 저장되면 둘 다 커밋된다")
        void salesAndOutbox_BothCommitted_WhenTransactionSucceeds() {
            // given
            String orderId = UUID.randomUUID().toString();

            // when
            SalesEntity savedSales = transactionTemplate.execute(status -> {
                SalesItemEntity item = SalesItemEntity.create(1L, "테스트 상품", 2, 10000);
                SalesEntity sales = SalesEntity.createWithItems(orderId, List.of(item));
                SalesEntity saved = salesRepository.save(sales);

                OutboxEntity outbox = OutboxEntity.create(
                        "ORDER",
                        String.valueOf(saved.getId()),
                        "ORDER_CREATED",
                        "{\"orderId\": \"" + orderId + "\"}",
                        "order-created",
                        orderId
                );
                outboxRepository.save(outbox);

                return saved;
            });

            // then
            assertThat(savedSales).isNotNull();
            assertThat(outboxRepository.findAll()).hasSize(1);

            OutboxEntity outbox = outboxRepository.findAll().get(0);
            assertThat(outbox.getAggregateId()).isEqualTo(String.valueOf(savedSales.getId()));
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.INIT);
        }

        @Test
        @DisplayName("트랜잭션 롤백 시 주문과 Outbox 모두 롤백된다")
        void salesAndOutbox_BothRolledBack_WhenTransactionFails() {
            // given
            String orderId = UUID.randomUUID().toString();
            long initialOutboxCount = outboxRepository.count();

            // when & then
            assertThatThrownBy(() -> {
                transactionTemplate.execute(status -> {
                    SalesItemEntity item = SalesItemEntity.create(1L, "테스트 상품", 2, 10000);
                    SalesEntity sales = SalesEntity.createWithItems(orderId, List.of(item));
                    SalesEntity saved = salesRepository.save(sales);

                    OutboxEntity outbox = OutboxEntity.create(
                            "ORDER",
                            String.valueOf(saved.getId()),
                            "ORDER_CREATED",
                            "{\"orderId\": \"" + orderId + "\"}",
                            "order-created",
                            orderId
                    );
                    outboxRepository.save(outbox);

                    // 강제로 예외 발생
                    throw new RuntimeException("트랜잭션 강제 롤백");
                });
            }).isInstanceOf(RuntimeException.class);

            // 롤백되어 Outbox가 저장되지 않음
            assertThat(outboxRepository.count()).isEqualTo(initialOutboxCount);
        }
    }

    @Nested
    @DisplayName("Outbox 상태 전이 테스트")
    class OutboxStateTransitionTest {

        @Test
        @DisplayName("생성 직후 INIT 상태이다")
        void initialState_IsInit() {
            // when
            OutboxEntity outbox = createAndSaveOutbox("order-1");

            // then
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.INIT);
            assertThat(outbox.getRetryCount()).isEqualTo(0);
            assertThat(outbox.getProcessedAt()).isNull();
        }

        @Test
        @DisplayName("성공 시 SEND_SUCCESS로 전이되고 processedAt이 설정된다")
        void success_TransitionsToSendSuccess() {
            // given
            OutboxEntity outbox = createAndSaveOutbox("order-1");

            // when
            transactionTemplate.execute(status -> {
                OutboxEntity found = outboxRepository.findById(outbox.getId()).orElseThrow();
                found.markAsSendSuccess();
                return outboxRepository.save(found);
            });

            // then
            OutboxEntity result = outboxRepository.findById(outbox.getId()).orElseThrow();
            assertThat(result.getStatus()).isEqualTo(OutboxStatus.SEND_SUCCESS);
            assertThat(result.getProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("실패 시 SEND_FAIL로 전이되고 retryCount가 증가한다")
        void failure_TransitionsToSendFail_AndIncrementsRetryCount() {
            // given
            OutboxEntity outbox = createAndSaveOutbox("order-1");

            // when
            transactionTemplate.execute(status -> {
                OutboxEntity found = outboxRepository.findById(outbox.getId()).orElseThrow();
                found.markAsSendFail("Kafka broker unavailable");
                return outboxRepository.save(found);
            });

            // then
            OutboxEntity result = outboxRepository.findById(outbox.getId()).orElseThrow();
            assertThat(result.getStatus()).isEqualTo(OutboxStatus.SEND_FAIL);
            assertThat(result.getRetryCount()).isEqualTo(1);
            assertThat(result.getErrorMessage()).isEqualTo("Kafka broker unavailable");
        }

        @Test
        @DisplayName("여러 번 실패 시 retryCount가 누적된다")
        void multipleFailures_AccumulateRetryCount() {
            // given
            OutboxEntity outbox = createAndSaveOutbox("order-1");

            // when - 3번 실패
            for (int i = 0; i < 3; i++) {
                final int attempt = i + 1;
                transactionTemplate.execute(status -> {
                    OutboxEntity found = outboxRepository.findById(outbox.getId()).orElseThrow();
                    found.markAsSendFail("Error attempt " + attempt);
                    return outboxRepository.save(found);
                });
            }

            // then
            OutboxEntity result = outboxRepository.findById(outbox.getId()).orElseThrow();
            assertThat(result.getRetryCount()).isEqualTo(3);
            assertThat(result.canRetry(3)).isFalse();
        }

        @Test
        @DisplayName("SEND_FAIL 상태에서 재시도 성공 시 SEND_SUCCESS로 전이된다")
        void retryAfterFail_TransitionsToSuccess() {
            // given
            OutboxEntity outbox = createAndSaveOutbox("order-1");

            // 먼저 실패
            transactionTemplate.execute(status -> {
                OutboxEntity found = outboxRepository.findById(outbox.getId()).orElseThrow();
                found.markAsSendFail("First failure");
                return outboxRepository.save(found);
            });

            // when - 재시도 성공
            transactionTemplate.execute(status -> {
                OutboxEntity found = outboxRepository.findById(outbox.getId()).orElseThrow();
                found.markAsSendSuccess();
                return outboxRepository.save(found);
            });

            // then
            OutboxEntity result = outboxRepository.findById(outbox.getId()).orElseThrow();
            assertThat(result.getStatus()).isEqualTo(OutboxStatus.SEND_SUCCESS);
            assertThat(result.getRetryCount()).isEqualTo(1);  // 이전 실패 횟수 유지
            assertThat(result.getProcessedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Outbox 페이로드 테스트")
    class OutboxPayloadTest {

        @Test
        @DisplayName("JSON 페이로드가 정확하게 저장된다")
        void jsonPayload_SavedCorrectly() throws Exception {
            // given
            String orderId = UUID.randomUUID().toString();
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(orderId)
                    .salesId(1L)
                    .totalAmount(50000)
                    .items(List.of(
                            OrderCreatedEvent.OrderItem.builder()
                                    .productId(1L)
                                    .productName("테스트 상품")
                                    .quantity(2)
                                    .price(25000)
                                    .build()
                    ))
                    .build();

            String payload = objectMapper.writeValueAsString(event);

            // when
            OutboxEntity outbox = OutboxEntity.create(
                    "ORDER",
                    "1",
                    "ORDER_CREATED",
                    payload,
                    "order-created",
                    orderId
            );
            OutboxEntity saved = outboxRepository.save(outbox);

            // then
            OutboxEntity result = outboxRepository.findById(saved.getId()).orElseThrow();
            OrderCreatedEvent deserialized = objectMapper.readValue(
                    result.getPayload(), OrderCreatedEvent.class
            );

            assertThat(deserialized.getOrderId()).isEqualTo(orderId);
            assertThat(deserialized.getSalesId()).isEqualTo(1L);
            assertThat(deserialized.getTotalAmount()).isEqualTo(50000);
            assertThat(deserialized.getItems()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("멱등성 테스트")
    class IdempotencyTest {

        @Test
        @DisplayName("동일한 aggregateId와 eventType으로 중복 저장할 수 있다")
        void allowDuplicateMessages() {
            // given
            createAndSaveOutbox("order-1");
            createAndSaveOutbox("order-1");

            // when
            List<OutboxEntity> messages = outboxRepository.findByAggregateIdAndEventTypeAndStatus(
                    "order-1", "ORDER_CREATED", OutboxStatus.INIT
            );

            // then - 중복 허용 (재시도 시나리오 대응)
            assertThat(messages).hasSize(2);
        }

        @Test
        @DisplayName("처리 완료된 메시지와 미처리 메시지를 구분할 수 있다")
        void distinguishProcessedAndUnprocessedMessages() {
            // given
            OutboxEntity processed = createAndSaveOutbox("order-1");
            processed.markAsSendSuccess();
            outboxRepository.save(processed);

            createAndSaveOutbox("order-1");

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

    private OutboxEntity createAndSaveOutbox(String aggregateId) {
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

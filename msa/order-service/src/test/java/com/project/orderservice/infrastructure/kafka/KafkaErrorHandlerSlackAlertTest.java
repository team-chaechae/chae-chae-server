package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.infrastructure.alert.SlackAlertService;
import com.project.orderservice.infrastructure.config.kafka.KafkaErrorHandlerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * KafkaErrorHandlerConfig의 Error Handler 테스트
 *
 * 목적: DefaultErrorHandler가 에러 발생 시 SlackAlertService를 호출하는지 검증
 * handleOne 메서드를 통해 실제 에러 핸들링 동작 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaErrorHandlerConfig - Slack 알림 테스트")
class KafkaErrorHandlerSlackAlertTest {

    @Mock
    private SlackAlertService slackAlertService;

    private KafkaErrorHandlerConfig kafkaErrorHandlerConfig;
    private DefaultErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        kafkaErrorHandlerConfig = new KafkaErrorHandlerConfig(slackAlertService);
        errorHandler = (DefaultErrorHandler) kafkaErrorHandlerConfig.kafkaErrorHandler();
    }

    @Test
    @DisplayName("KafkaErrorHandlerConfig가 SlackAlertService를 주입받아 ErrorHandler를 생성한다")
    void kafkaErrorHandlerConfig_ShouldCreateErrorHandler_WithSlackAlertService() {
        // given & when
        CommonErrorHandler handler = kafkaErrorHandlerConfig.kafkaErrorHandler();

        // then
        assertThat(handler).isNotNull();
        assertThat(handler).isInstanceOf(DefaultErrorHandler.class);
    }

    @Test
    @DisplayName("ErrorHandler가 재시도 설정을 가진다 - 3번 시도 후 recoverer 호출")
    void kafkaErrorHandler_ShouldHaveRetryConfiguration() {
        // given
        ConsumerRecord<String, String> record = createConsumerRecord("test-topic", "test-key", "test-value");
        RuntimeException testException = new RuntimeException("테스트 예외");

        // when - 재시도 가능한 예외로 3번 시도 (초기 1회 + 재시도 2회)
        boolean first = errorHandler.handleOne(testException, record, null, null);
        boolean second = errorHandler.handleOne(testException, record, null, null);
        boolean third = errorHandler.handleOne(testException, record, null, null);

        // then - 처음 2번은 false (재시도), 3번째는 true (최종 실패 → recoverer 호출)
        assertThat(first).isFalse();
        assertThat(second).isFalse();
        assertThat(third).isTrue();

        // recoverer가 호출되어 Slack 알림 발송
        verify(slackAlertService, times(1))
                .sendKafkaErrorAlert(anyString(), anyString(), any(Exception.class));
    }

    @Test
    @DisplayName("DeserializationException이 재시도 불가 예외로 설정되어 있다")
    void kafkaErrorHandler_ShouldMarkDeserializationException_AsNotRetryable() {
        // given
        ConsumerRecord<String, String> record = createConsumerRecord("payment-completed", "key-1", "value-1");
        DeserializationException deserializationException =
                new DeserializationException("역직렬화 실패", new byte[]{}, false, new RuntimeException("JSON parse error"));

        // when - handleOne은 재시도 불가 예외를 처리
        // DeserializationException은 재시도하지 않고 바로 recoverer로 전달됨
        boolean result = errorHandler.handleOne(deserializationException, record, null, null);

        // then - true 반환 = 처리 완료 (재시도 없이)
        assertThat(result).isTrue();

        // Slack 알림 호출 확인
        verify(slackAlertService, times(1))
                .sendKafkaErrorAlert(eq("payment-completed"), anyString(), any(DeserializationException.class));
    }

    @Test
    @DisplayName("SerializationException도 재시도 불가 예외로 설정되어 있다")
    void kafkaErrorHandler_ShouldMarkSerializationException_AsNotRetryable() {
        // given
        ConsumerRecord<String, String> record = createConsumerRecord("order-created", "order-123", "value");
        org.apache.kafka.common.errors.SerializationException serializationException =
                new org.apache.kafka.common.errors.SerializationException("직렬화 실패");

        // when
        boolean result = errorHandler.handleOne(serializationException, record, null, null);

        // then
        assertThat(result).isTrue();

        verify(slackAlertService, times(1))
                .sendKafkaErrorAlert(eq("order-created"), contains("order-123"), any(Exception.class));
    }

    @Test
    @DisplayName("일반 RuntimeException은 재시도 후 Slack 알림 발송")
    void kafkaErrorHandler_ShouldRetryAndThenSendSlackAlert_ForRuntimeException() {
        // given
        ConsumerRecord<String, String> record = createConsumerRecord("payment-completed", "order-456", "test-value");
        RuntimeException runtimeException = new RuntimeException("DB 연결 실패");

        // when - handleOne은 재시도 가능한 예외의 경우 false 반환 (재시도 필요)
        // 첫 번째 시도
        boolean firstResult = errorHandler.handleOne(runtimeException, record, null, null);

        // then - 첫 번째는 false (재시도 필요)
        assertThat(firstResult).isFalse();

        // 두 번째 시도
        boolean secondResult = errorHandler.handleOne(runtimeException, record, null, null);
        assertThat(secondResult).isFalse();

        // 세 번째 시도 (maxAttempts 초과로 recoverer 호출)
        boolean thirdResult = errorHandler.handleOne(runtimeException, record, null, null);
        assertThat(thirdResult).isTrue();

        // Slack 알림은 최종 실패 시에만 호출
        verify(slackAlertService, times(1))
                .sendKafkaErrorAlert(eq("payment-completed"), anyString(), any(RuntimeException.class));
    }

    @Test
    @DisplayName("Slack 알림에 topic, key, exception 정보가 포함된다")
    void kafkaErrorHandler_ShouldIncludeDetailedInfo_InSlackAlert() {
        // given
        ConsumerRecord<String, String> record = createConsumerRecord("payment-completed", "order-789", "test");
        DeserializationException exception =
                new DeserializationException("JSON 파싱 실패", new byte[]{}, false, new RuntimeException("상세 에러"));

        // when
        errorHandler.handleOne(exception, record, null, null);

        // then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        verify(slackAlertService).sendKafkaErrorAlert(
                topicCaptor.capture(),
                messageCaptor.capture(),
                exceptionCaptor.capture()
        );

        assertThat(topicCaptor.getValue()).isEqualTo("payment-completed");
        assertThat(messageCaptor.getValue()).contains("order-789");
        assertThat(exceptionCaptor.getValue()).isInstanceOf(DeserializationException.class);
    }

    private ConsumerRecord<String, String> createConsumerRecord(String topic, String key, String value) {
        return new ConsumerRecord<>(
                topic,           // topic
                0,               // partition
                0L,              // offset
                System.currentTimeMillis(),  // timestamp
                TimestampType.CREATE_TIME,   // timestamp type
                key.length(),    // serialized key size
                value.length(),  // serialized value size
                key,             // key
                value,           // value
                new org.apache.kafka.common.header.internals.RecordHeaders(),  // headers
                java.util.Optional.empty()   // leader epoch
        );
    }
}

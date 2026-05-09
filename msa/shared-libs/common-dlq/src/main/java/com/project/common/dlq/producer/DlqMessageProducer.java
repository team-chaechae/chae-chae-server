package com.project.common.dlq.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.dlq.config.DlqProperties;
import com.project.common.dlq.domain.DlqMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * DLQ 메시지 Producer
 *
 * DLQ 토픽으로 실패한 메시지를 발행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DlqMessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final DlqProperties properties;

    /**
     * DLQ 토픽으로 메시지 발행
     *
     * @param message DLQ 메시지
     */
    public void sendToDlq(DlqMessage message) {
        String dlqTopic = resolveDlqTopic(message.getOriginalTopic());

        try {
            String payload = objectMapper.writeValueAsString(message);

            kafkaTemplate.send(dlqTopic, message.getOriginalKey(), payload)
                    .get(10, TimeUnit.SECONDS);

            log.info("[DLQ Producer] DLQ 발행 성공 - topic: {}, key: {}",
                    dlqTopic, message.getOriginalKey());

        } catch (JsonProcessingException e) {
            log.error("[DLQ Producer] 메시지 직렬화 실패 - topic: {}, error: {}",
                    message.getOriginalTopic(), e.getMessage());
            throw new IllegalStateException("DLQ 메시지 직렬화 실패", e);
        } catch (Exception e) {
            log.error("[DLQ Producer] DLQ 발행 실패 - topic: {}, key: {}, error: {}",
                    dlqTopic, message.getOriginalKey(), resolveErrorMessage(e));
            throw new IllegalStateException("DLQ 발행 실패", e);
        }
    }

    /**
     * 원본 토픽으로 메시지 재발행 (리플레이) - 비동기
     *
     * @param message DLQ 메시지
     */
    public void replayToOriginal(DlqMessage message) {
        String originalTopic = message.getOriginalTopic();

        kafkaTemplate.send(originalTopic, message.getOriginalKey(), message.getOriginalPayload())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[DLQ Replay] 재발행 실패 - topic: {}, key: {}, error: {}",
                                originalTopic, message.getOriginalKey(), ex.getMessage());
                    } else {
                        log.info("[DLQ Replay] 재발행 성공 - topic: {}, partition: {}, offset: {}",
                                originalTopic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * 원본 토픽으로 메시지 재발행 (리플레이) - 동기
     * 실패 시 false 반환하여 ack하지 않도록 함
     *
     * @param message DLQ 메시지
     * @return 발행 성공 여부
     */
    public boolean replayToOriginalSync(DlqMessage message) {
        String originalTopic = message.getOriginalTopic();

        try {
            kafkaTemplate.send(originalTopic, message.getOriginalKey(), message.getOriginalPayload())
                    .get(10, TimeUnit.SECONDS);

            log.info("[DLQ Replay] 동기 재발행 성공 - topic: {}, key: {}",
                    originalTopic, message.getOriginalKey());
            return true;

        } catch (Exception e) {
            log.error("[DLQ Replay] 동기 재발행 실패 - topic: {}, key: {}, error: {}",
                    originalTopic, message.getOriginalKey(), e.getMessage());
            return false;
        }
    }

    /**
     * DLQ 토픽명 생성
     *
     * @param originalTopic 원본 토픽명
     * @return DLQ 토픽명
     */
    public String resolveDlqTopic(String originalTopic) {
        return originalTopic + properties.getTopicSuffix();
    }

    private String resolveErrorMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getMessage();
        }
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return message;
    }
}

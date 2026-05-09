package com.project.common.dlq.handler;

import com.project.common.dlq.alert.DlqAlertService;
import com.project.common.dlq.config.DlqProperties;
import com.project.common.dlq.domain.DlqMessage;
import com.project.common.dlq.domain.DlqRecord;
import com.project.common.dlq.domain.DlqRecordRepository;
import com.project.common.dlq.exception.DlqExceptionClassifier;
import com.project.common.dlq.producer.DlqMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DLQ Recoverer
 *
 * 재시도 실패 후 메시지를 DLQ로 전송하고, 비즈니스 오류는 DB에도 저장합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class DlqRecoverer implements ConsumerRecordRecoverer {

    private final DlqMessageProducer dlqProducer;
    private final DlqAlertService alertService;
    private final DlqExceptionClassifier classifier;
    private final DlqRecordRepository dlqRecordRepository;
    private final DlqProperties properties;
    private final String serviceName;

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception exception) {
        log.error("[DLQ Recoverer] 메시지 복구 시작 - topic: {}, partition: {}, offset: {}",
                record.topic(), record.partition(), record.offset());

        // DLQ 메시지 생성
        DlqMessage dlqMessage = buildDlqMessage(record, exception);

        // 1. DLQ 토픽으로 발행
        dlqProducer.sendToDlq(dlqMessage);

        // 2. 비즈니스 오류인 경우 DB에도 저장
        if (properties.isDbEnabled() &&
                dlqMessage.getExceptionCategory() == DlqMessage.ExceptionCategory.BUSINESS) {
            saveToDatabaseSafely(dlqMessage);
        }

        // 3. 알림 전송
        alertService.sendDlqAlert(dlqMessage);

        log.info("[DLQ Recoverer] 복구 완료 - topic: {}, category: {}, exception: {}",
                record.topic(), dlqMessage.getExceptionCategory(), dlqMessage.getExceptionType());
    }

    /**
     * DLQ 메시지 생성
     */
    private DlqMessage buildDlqMessage(ConsumerRecord<?, ?> record, Exception exception) {
        String key = record.key() != null ? record.key().toString() : null;
        String payload = extractPayload(record);
        Map<String, String> headers = extractHeaders(record);

        return DlqMessage.builder()
                .originalTopic(record.topic())
                .originalPartition(record.partition())
                .originalOffset(record.offset())
                .originalKey(key)
                .originalPayload(payload)
                .exceptionCategory(classifier.classify(exception))
                .exceptionType(classifier.getExceptionType(exception))
                .exceptionMessage(classifier.getExceptionMessage(exception))
                .stackTrace(getStackTrace(exception))
                .timestamp(LocalDateTime.now())
                .retryCount(0)
                .serviceName(serviceName)
                .headers(headers)
                .build();
    }

    /**
     * 레코드에서 페이로드 추출
     */
    private String extractPayload(ConsumerRecord<?, ?> record) {
        if (record.value() == null) {
            return null;
        }

        if (record.value() instanceof String) {
            return (String) record.value();
        }

        if (record.value() instanceof byte[]) {
            return new String((byte[]) record.value(), StandardCharsets.UTF_8);
        }

        return record.value().toString();
    }

    /**
     * 레코드에서 헤더 추출
     */
    private Map<String, String> extractHeaders(ConsumerRecord<?, ?> record) {
        Map<String, String> headers = new HashMap<>();
        record.headers().forEach(header -> {
            String value = header.value() != null
                    ? new String(header.value(), StandardCharsets.UTF_8)
                    : null;
            headers.put(header.key(), value);
        });
        return headers;
    }

    /**
     * 스택 트레이스 문자열 변환
     */
    private String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String stackTrace = sw.toString();

        // 스택 트레이스가 너무 길면 자르기
        if (stackTrace.length() > 10000) {
            return stackTrace.substring(0, 10000) + "\n... (truncated)";
        }
        return stackTrace;
    }

    /**
     * DB 저장 (안전하게)
     */
    private void saveToDatabaseSafely(DlqMessage message) {
        try {
            DlqRecord record = DlqRecord.from(message);
            dlqRecordRepository.save(record);
            log.debug("[DLQ Recoverer] DB 저장 완료 - id: {}", record.getId());
        } catch (Exception e) {
            log.error("[DLQ Recoverer] DB 저장 실패 - topic: {}, error: {}",
                    message.getOriginalTopic(), e.getMessage());
            // DB 저장 실패해도 DLQ 토픽 발행은 성공했으므로 계속 진행
        }
    }
}

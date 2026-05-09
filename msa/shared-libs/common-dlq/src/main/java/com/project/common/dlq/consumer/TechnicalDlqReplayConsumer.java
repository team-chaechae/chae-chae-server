package com.project.common.dlq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.dlq.alert.DlqAlertService;
import com.project.common.dlq.config.DlqProperties;
import com.project.common.dlq.domain.DlqMessage;
import com.project.common.dlq.producer.DlqMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 기술적 오류 DLQ 자동 리플레이 Consumer
 *
 * DLQ 토픽에서 자기 서비스의 기술적 오류 메시지만 자동으로 원본 토픽으로 재발행합니다.
 * - serviceName 필터링으로 자기 서비스 DLQ만 처리
 * - 동기 전송으로 실패 시 ack하지 않음
 * - 최대 재시도 횟수 초과 시 알림을 전송하고 메시지를 폐기합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "dlq.technical-replay", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TechnicalDlqReplayConsumer {

    private final DlqMessageProducer dlqProducer;
    private final DlqAlertService alertService;
    private final DlqProperties properties;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    /**
     * DLQ 토픽 메시지 처리
     *
     * 패턴: *.dlq (모든 DLQ 토픽)
     * 필터링: 자기 서비스의 DLQ 메시지만 처리
     */
    @KafkaListener(
            topicPattern = ".*\\.dlq",
            groupId = "${spring.application.name}-dlq-replay",
            containerFactory = "dlqReplayListenerFactory",
            autoStartup = "${dlq.technical-replay.enabled:true}"
    )
    public void handleDlqMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String dlqTopic = record.topic();
        long offset = record.offset();

        try {
            DlqMessage message = objectMapper.readValue(record.value(), DlqMessage.class);

            // 자기 서비스의 DLQ 메시지만 처리 (다른 서비스 메시지는 스킵)
            if (!serviceName.equals(message.getServiceName())) {
                log.debug("[DLQ Replay] 다른 서비스 메시지 스킵 - myService: {}, messageService: {}, topic: {}",
                        serviceName, message.getServiceName(), dlqTopic);
                ack.acknowledge();
                return;
            }

            // 기술적 오류만 자동 리플레이
            if (message.getExceptionCategory() != DlqMessage.ExceptionCategory.TECHNICAL) {
                log.debug("[DLQ Replay] 비즈니스 오류 스킵 - topic: {}, offset: {}",
                        dlqTopic, offset);
                ack.acknowledge();
                return;
            }

            int maxReplayRetries = properties.getTechnicalReplay().getMaxReplayRetries();

            // 최대 재시도 횟수 초과 확인
            if (message.getRetryCount() >= maxReplayRetries) {
                log.warn("[DLQ Replay] 최대 재시도 초과 - topic: {}, offset: {}, retryCount: {}",
                        dlqTopic, offset, message.getRetryCount());

                // 알림 전송
                alertService.sendTechnicalReplayFailedAlert(message, message.getRetryCount());

                // 메시지 폐기 (ack하고 끝)
                ack.acknowledge();
                return;
            }

            // 재시도 횟수 증가
            message.setRetryCount(message.getRetryCount() + 1);

            // 원본 토픽으로 동기 재발행 (실패 시 ack 안함)
            log.info("[DLQ Replay] 재발행 시도 - originalTopic: {}, key: {}, retryCount: {}",
                    message.getOriginalTopic(), message.getOriginalKey(), message.getRetryCount());

            boolean success = dlqProducer.replayToOriginalSync(message);

            if (success) {
                ack.acknowledge();
                log.info("[DLQ Replay] 재발행 완료 - originalTopic: {}, offset: {}",
                        message.getOriginalTopic(), offset);
            } else {
                // 증가된 retryCount를 DLQ payload에 반영해야 최대 재시도 횟수가 동작한다.
                try {
                    dlqProducer.sendToDlq(message);
                    ack.acknowledge();
                    log.warn("[DLQ Replay] 재발행 실패 - retryCount 증가 후 DLQ 재등록 - topic: {}, offset: {}, retryCount: {}",
                            message.getOriginalTopic(), offset, message.getRetryCount());
                } catch (Exception requeueException) {
                    log.error("[DLQ Replay] retryCount 증가 메시지 DLQ 재등록 실패 - ack 안함, topic: {}, offset: {}, error: {}",
                            message.getOriginalTopic(), offset, requeueException.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("[DLQ Replay] 처리 실패 - topic: {}, offset: {}, error: {}",
                    dlqTopic, offset, e.getMessage());

            // 파싱 실패 등은 ack하고 넘어감 (무한 루프 방지)
            ack.acknowledge();
        }
    }
}

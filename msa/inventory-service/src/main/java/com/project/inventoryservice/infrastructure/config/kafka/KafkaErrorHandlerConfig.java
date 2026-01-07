package com.project.inventoryservice.infrastructure.config.kafka;

import com.project.common.dlq.handler.DlqErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.listener.CommonErrorHandler;

/**
 * Kafka Error Handler 설정
 *
 * DLQ 모듈의 DlqErrorHandler를 사용하여 에러 처리 및 DLQ 발행을 수행합니다.
 * - 기술적 오류: 재시도 후 DLQ (자동 리플레이 대상)
 * - 비즈니스 오류: 즉시 DLQ + DB 저장 (수동 확인)
 */
@Slf4j
@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    @Primary
    public CommonErrorHandler kafkaErrorHandler(DlqErrorHandler dlqErrorHandler) {
        log.info("[Kafka Config] DLQ ErrorHandler 설정 완료");
        return dlqErrorHandler;
    }
}

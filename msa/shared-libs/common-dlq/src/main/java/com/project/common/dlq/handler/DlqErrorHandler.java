package com.project.common.dlq.handler;

import com.project.common.dlq.config.DlqProperties;
import com.project.common.dlq.exception.DlqExceptionClassifier;
import com.project.common.dlq.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * DLQ Error Handler
 *
 * 예외 유형에 따라 차별화된 재시도 및 복구 전략을 적용합니다.
 *
 * - 기술적 오류: ExponentialBackOff로 재시도 → 실패 시 DLQ 토픽 (자동 리플레이 대상)
 * - 비즈니스 오류: 재시도 없음 → 즉시 DLQ 토픽 + DB 저장 (수동 확인)
 * - 역직렬화 오류: 재시도 없음 → 즉시 DLQ (poison pill)
 */
@Slf4j
public class DlqErrorHandler extends DefaultErrorHandler {

    private final DlqExceptionClassifier classifier;

    public DlqErrorHandler(
            DlqRecoverer recoverer,
            DlqExceptionClassifier classifier,
            DlqProperties properties) {

        // Exponential Backoff 설정
        super(recoverer, createBackOff(properties));

        this.classifier = classifier;

        // 재시도 불가 예외 설정 (즉시 Recoverer로)
        configureNotRetryableExceptions();

        log.info("[DLQ ErrorHandler] 초기화 완료 - maxRetries: {}, interval: {}ms, multiplier: {}",
                properties.getMaxRetries(),
                properties.getRetryIntervalMs(),
                properties.getRetryMultiplier());
    }

    /**
     * Exponential BackOff 생성
     */
    private static ExponentialBackOff createBackOff(DlqProperties properties) {
        ExponentialBackOff backOff = new ExponentialBackOff(
                properties.getRetryIntervalMs(),
                properties.getRetryMultiplier()
        );
        // 최대 재시도 시간 = 초기간격 * 배수^재시도횟수 * 2
        long maxElapsedTime = (long) (properties.getRetryIntervalMs()
                * Math.pow(properties.getRetryMultiplier(), properties.getMaxRetries())
                * 2);
        backOff.setMaxElapsedTime(maxElapsedTime);
        return backOff;
    }

    /**
     * 재시도 불가 예외 설정
     */
    private void configureNotRetryableExceptions() {
        // 역직렬화 오류 (poison pill) - 재시도 불가
        this.addNotRetryableExceptions(
                DeserializationException.class,
                SerializationException.class
        );

        // 비즈니스 예외 - 재시도 불가
        this.addNotRetryableExceptions(BusinessException.class);

        // 프로그래밍 오류 - 재시도 불가
        this.addNotRetryableExceptions(
                NullPointerException.class,
                IllegalArgumentException.class,
                IllegalStateException.class
        );
    }
}

package com.project.common.dlq.exception;

import com.project.common.dlq.domain.DlqMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * 예외 분류기
 *
 * 예외를 기술적 오류(자동 리플레이)와 비즈니스 오류(수동 확인)로 분류합니다.
 * 클래스명 기반으로 체크하여 각 서비스의 BusinessException을 모두 인식합니다.
 */
@Slf4j
@Component
public class DlqExceptionClassifier {

    /**
     * 비즈니스 예외 클래스명 패턴 (각 서비스의 예외를 클래스명으로 매칭)
     */
    private static final Set<String> BUSINESS_EXCEPTION_NAMES = Set.of(
            "BusinessException",
            "BadRequestException",
            "EntityNotFoundException",
            "EntityAlreadyExistException",
            "PaymentFailedException",
            "InsufficientStockException",
            "NotFoundException"
    );

    /**
     * 기술적 오류 예외 클래스 목록 (자동 리플레이 대상)
     */
    private static final Set<Class<? extends Throwable>> TECHNICAL_EXCEPTIONS = Set.of(
            // 네트워크 관련
            ConnectException.class,
            SocketTimeoutException.class,
            IOException.class,

            // 타임아웃
            TimeoutException.class,

            // Kafka 관련
            KafkaException.class,

            // DB 일시적 오류
            TransientDataAccessException.class,

            // 명시적 기술적 오류
            TechnicalException.class
    );

    /**
     * 재시도 불가 예외 클래스 목록 (즉시 DLQ로)
     */
    private static final Set<Class<? extends Throwable>> NON_RETRYABLE_EXCEPTIONS = Set.of(
            // 역직렬화 오류 (poison pill)
            DeserializationException.class,
            org.apache.kafka.common.errors.SerializationException.class,

            // 프로그래밍 오류
            NullPointerException.class,
            IllegalArgumentException.class,
            IllegalStateException.class
    );

    /**
     * 예외 분류
     *
     * 전체 cause 체인을 탐색하여 BusinessException이 있으면 비즈니스 오류로 분류합니다.
     * RuntimeException으로 감싸져 있어도 원본 예외를 정확히 분류합니다.
     *
     * @param exception 발생한 예외
     * @return 예외 카테고리 (TECHNICAL 또는 BUSINESS)
     */
    public DlqMessage.ExceptionCategory classify(Throwable exception) {
        // 전체 cause 체인에서 BusinessException 확인 (래핑 예외 대응)
        if (containsBusinessException(exception)) {
            Throwable businessEx = findBusinessException(exception);
            log.debug("[DLQ Classifier] 비즈니스 오류 - {}: {}",
                    businessEx.getClass().getSimpleName(), businessEx.getMessage());
            return DlqMessage.ExceptionCategory.BUSINESS;
        }

        Throwable rootCause = getRootCause(exception);

        // 기술적 오류인지 확인
        if (isTechnicalException(rootCause)) {
            log.debug("[DLQ Classifier] 기술적 오류 - {}: {}",
                    rootCause.getClass().getSimpleName(), rootCause.getMessage());
            return DlqMessage.ExceptionCategory.TECHNICAL;
        }

        // 분류 불가 → 비즈니스 오류로 처리 (수동 확인 필요)
        log.debug("[DLQ Classifier] 분류 불가 (비즈니스로 처리) - {}: {}",
                rootCause.getClass().getSimpleName(), rootCause.getMessage());
        return DlqMessage.ExceptionCategory.BUSINESS;
    }

    /**
     * 재시도 가능 여부 판단
     *
     * @param exception 발생한 예외
     * @return 재시도 가능 여부
     */
    public boolean isRetryable(Throwable exception) {
        // 전체 cause 체인에서 재시도 불가 예외 확인
        Throwable current = exception;
        while (current != null) {
            // 재시도 불가 예외인지 확인
            for (Class<? extends Throwable> clazz : NON_RETRYABLE_EXCEPTIONS) {
                if (clazz.isAssignableFrom(current.getClass())) {
                    log.debug("[DLQ Classifier] 재시도 불가 - {}", current.getClass().getSimpleName());
                    return false;
                }
            }

            // 비즈니스 예외는 재시도 불가 (클래스명으로 체크)
            if (isBusinessException(current)) {
                log.debug("[DLQ Classifier] 비즈니스 예외 - 재시도 불가");
                return false;
            }

            current = current.getCause();
            if (current == exception) break; // 순환 참조 방지
        }

        // 기술적 오류는 재시도 가능
        Throwable rootCause = getRootCause(exception);
        return isTechnicalException(rootCause);
    }

    /**
     * cause 체인에 BusinessException이 포함되어 있는지 확인 (클래스명 기반)
     */
    private boolean containsBusinessException(Throwable exception) {
        return findBusinessException(exception) != null;
    }

    /**
     * cause 체인에서 BusinessException 찾기 (클래스명 기반)
     */
    private Throwable findBusinessException(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (isBusinessException(current)) {
                return current;
            }
            Throwable next = current.getCause();
            if (next == current) break; // 순환 참조 방지
            current = next;
        }
        return null;
    }

    /**
     * 비즈니스 예외인지 클래스명으로 확인
     */
    private boolean isBusinessException(Throwable exception) {
        String className = exception.getClass().getSimpleName();
        return BUSINESS_EXCEPTION_NAMES.contains(className);
    }

    /**
     * 기술적 오류 여부 확인
     */
    private boolean isTechnicalException(Throwable exception) {
        for (Class<? extends Throwable> clazz : TECHNICAL_EXCEPTIONS) {
            if (clazz.isAssignableFrom(exception.getClass())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 루트 원인 예외 추출
     */
    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * 예외 유형명 추출
     */
    public String getExceptionType(Throwable exception) {
        Throwable rootCause = getRootCause(exception);
        return rootCause.getClass().getSimpleName();
    }

    /**
     * 예외 메시지 추출 (안전하게)
     */
    public String getExceptionMessage(Throwable exception) {
        Throwable rootCause = getRootCause(exception);
        String message = rootCause.getMessage();
        return message != null ? message : rootCause.getClass().getSimpleName();
    }
}

package com.project.common.dlq.exception;

/**
 * 기술적 오류 마커 예외
 *
 * 이 예외 또는 하위 예외가 발생하면 DLQ에서 자동 리플레이 대상이 됩니다.
 * - 네트워크 오류
 * - 타임아웃
 * - 일시적 DB 오류
 * - Kafka 일시적 오류
 */
public class TechnicalException extends RuntimeException {

    public TechnicalException(String message) {
        super(message);
    }

    public TechnicalException(String message, Throwable cause) {
        super(message, cause);
    }

    public TechnicalException(Throwable cause) {
        super(cause);
    }
}

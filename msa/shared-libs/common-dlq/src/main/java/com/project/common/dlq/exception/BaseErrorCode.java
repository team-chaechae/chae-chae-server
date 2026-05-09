package com.project.common.dlq.exception;

import org.springframework.http.HttpStatus;

/**
 * DLQ 분류용 ErrorCode 인터페이스
 */
public interface BaseErrorCode {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}

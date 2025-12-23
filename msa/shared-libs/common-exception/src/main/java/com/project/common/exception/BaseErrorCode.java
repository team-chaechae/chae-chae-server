package com.project.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 서비스별 ErrorCode enum이 구현해야 하는 인터페이스
 */
public interface BaseErrorCode {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}

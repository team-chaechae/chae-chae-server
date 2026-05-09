package com.project.common.dlq.exception;

public class BadRequestException extends BusinessException {

    public BadRequestException(BaseErrorCode errorCode) {
        super(errorCode);
    }

    public BadRequestException(BaseErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

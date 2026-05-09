package com.project.common.dlq.exception;

public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException(BaseErrorCode errorCode) {
        super(errorCode);
    }

    public EntityNotFoundException(BaseErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

package com.project.common.dlq.exception;

public class EntityAlreadyExistException extends BusinessException {

    public EntityAlreadyExistException(BaseErrorCode errorCode) {
        super(errorCode);
    }

    public EntityAlreadyExistException(BaseErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

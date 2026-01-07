package com.project.inventoryservice.application.global.exception;

public class BusinessException extends com.project.common.dlq.exception.BusinessException {

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    @Override
    public ErrorCode getErrorCode() {
        return (ErrorCode) super.getErrorCode();
    }
}

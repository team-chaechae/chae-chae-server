package com.project.common.dlq.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {

    private final String code;
    private final String message;

    public static ErrorResponse of(BaseErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }

    public static ErrorResponse of(BaseErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message);
    }
}

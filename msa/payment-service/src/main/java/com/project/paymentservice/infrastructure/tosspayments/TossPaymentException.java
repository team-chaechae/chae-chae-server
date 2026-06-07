package com.project.paymentservice.infrastructure.tosspayments;

import lombok.Getter;

@Getter
public class TossPaymentException extends RuntimeException {

    private final String code;
    private final int statusCode;

    public TossPaymentException(String code, String message, int statusCode) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
    }

    public TossPaymentException(String code, String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.statusCode = statusCode;
    }
}

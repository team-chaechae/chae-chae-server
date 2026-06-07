package com.project.paymentservice.infrastructure.order;

public class OrderSalesClientException extends RuntimeException {

    public OrderSalesClientException(String message) {
        super(message);
    }

    public OrderSalesClientException(String message, Throwable cause) {
        super(message, cause);
    }
}

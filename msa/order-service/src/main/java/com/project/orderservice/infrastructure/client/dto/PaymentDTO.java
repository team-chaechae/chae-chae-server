package com.project.orderservice.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class PaymentDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String orderId;
        private Long salesId;
        private Integer amount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private PaymentInfo payment;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PaymentInfo {
            private Long id;
            private Long salesId;
            private Integer amount;
            private String status;
            private String transactionId;
        }
    }
}

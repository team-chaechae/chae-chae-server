package com.project.orderservice.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class StockReservationDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReserveRequest {
        private String orderId;
        private Long salesId;
        private List<ReserveItem> items;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ReserveItem {
            private Long productId;
            private Integer quantity;
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReserveResponse {
        private String orderId;
        private Long salesId;
        private boolean success;
        private String failureReason;
        private List<ItemResult> items;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ItemResult {
            private Long productId;
            private Integer requestedQuantity;
            private Integer reservedQuantity;
            private Integer availableStock;
            private boolean success;
            private String errorReason;
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmRequest {
        private String orderId;
        private Long salesId;
        private List<ConfirmItem> items;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ConfirmItem {
            private Long productId;
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmResponse {
        private String orderId;
        private Long salesId;
        private boolean success;
        private String message;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReleaseRequest {
        private String orderId;
        private Long salesId;
        private List<ReleaseItem> items;
        private String reason;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ReleaseItem {
            private Long productId;
            private Integer quantity;
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReleaseResponse {
        private String orderId;
        private Long salesId;
        private boolean success;
        private String message;
    }
}

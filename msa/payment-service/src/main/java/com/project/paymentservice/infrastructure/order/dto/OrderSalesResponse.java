package com.project.paymentservice.infrastructure.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderSalesResponse(
        Integer code,
        String message,
        SalesData data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SalesData(
            SalesDetail sales
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SalesDetail(
            Long salesId,
            String orderId,
            String status,
            List<SalesItemDetail> items,
            Integer totalQuantity,
            Integer totalPrice
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SalesItemDetail(
            Long itemId,
            Long productId,
            String productName,
            Integer quantity,
            Integer price,
            Integer totalPrice
    ) {
    }
}

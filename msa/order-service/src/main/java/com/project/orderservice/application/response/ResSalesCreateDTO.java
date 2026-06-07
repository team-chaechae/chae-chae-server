package com.project.orderservice.application.response;

import com.project.orderservice.domain.model.SalesEntity;
import com.project.orderservice.domain.model.SalesItemEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResSalesCreateDTO {

    @Schema(example = "101")
    private Long salesId;

    @Schema(example = "c9f0b1ea-ef4a-4af8-aacf-54a1d7c3e16e")
    private String orderId;

    @Schema(example = "PENDING")
    private String status;

    private List<SalesItemInfo> items;
    private int totalCount;
    private int totalPrice;
    private LocalDateTime createdAt;

    public static ResSalesCreateDTO from(SalesEntity sales) {
        List<SalesItemInfo> itemInfoList = sales.getItems().stream()
                .map(SalesItemInfo::from)
                .toList();

        return ResSalesCreateDTO.builder()
                .salesId(sales.getId())
                .orderId(sales.getOrderId())
                .status(sales.getStatus().name())
                .items(itemInfoList)
                .totalCount(sales.getItems().size())
                .totalPrice(sales.getTotalPrice())
                .createdAt(sales.getCreatedAt())
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesItemInfo {

        @Schema(example = "201")
        private Long itemId;

        @Schema(example = "53")
        private Long productId;

        @Schema(example = "유기농 사과 5kg")
        private String productName;

        @Schema(example = "3")
        private int quantity;

        @Schema(example = "15000")
        private int price;

        @Schema(example = "45000")
        private int totalPrice;

        public static SalesItemInfo from(SalesItemEntity item) {
            return SalesItemInfo.builder()
                    .itemId(item.getId())
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .totalPrice(item.getTotalPrice())
                    .build();
        }
    }
}

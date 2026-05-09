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
public class ResSalesGetByIdDTO {

    private SalesDetail sales;

    public static ResSalesGetByIdDTO from(SalesEntity salesEntity) {
        return ResSalesGetByIdDTO.builder()
                .sales(SalesDetail.from(salesEntity))
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesDetail {

        @Schema(example = "101")
        private Long salesId;

        @Schema(example = "COMPLETED")
        private String status;

        private List<SalesItemDetail> items;

        private int totalQuantity;

        private int totalPrice;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;

        public static SalesDetail from(SalesEntity sales) {
            List<SalesItemDetail> itemDetails = sales.getItems().stream()
                    .map(SalesItemDetail::from)
                    .toList();

            return SalesDetail.builder()
                    .salesId(sales.getId())
                    .status(sales.getStatus().name())
                    .items(itemDetails)
                    .totalQuantity(sales.getTotalQuantity())
                    .totalPrice(sales.getTotalPrice())
                    .createdAt(sales.getCreatedAt())
                    .updatedAt(sales.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesItemDetail {

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

        public static SalesItemDetail from(SalesItemEntity item) {
            return SalesItemDetail.builder()
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

package com.project.chaechaeserver.application.response.sales;

import com.project.chaechaeserver.domain.model.sales.SalesEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResSalesGetByIdDTO {

    private Sales sales;

    public static ResSalesGetByIdDTO from(SalesEntity salesEntity) {
        return ResSalesGetByIdDTO.builder()
                .sales(Sales.from(salesEntity))
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sales {

        @Schema(example = "101")
        private Long salesId;

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

        private LocalDateTime createdAt;

        public static Sales from(SalesEntity salesEntity) {

            int quantity = salesEntity.getQuantity();
            int price = salesEntity.getPrice();

            return Sales.builder()
                    .salesId(salesEntity.getId())
                    .productId(salesEntity.getProductEntity().getId())
                    .productName(salesEntity.getProductEntity().getName())
                    .quantity(quantity)
                    .price(price)
                    .totalPrice(price * quantity)
                    .createdAt(salesEntity.getCreatedAt())
                    .build();
        }
    }
}
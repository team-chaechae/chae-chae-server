package com.project.chaechaeserver.application.response.sales;

import com.project.chaechaeserver.domain.model.sales.SalesEntity;
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

    public static ResSalesGetByIdDTO of(SalesEntity salesEntity) {
        return ResSalesGetByIdDTO.builder()
                .sales(Sales.from(salesEntity))
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sales {

        private Long salesId;
        private Long productId;
        private String productName;
        private int quantity;
        private int price;
        private int totalPrice;
        private LocalDateTime createdAt;

        public static Sales from(SalesEntity salesEntity) {

            int quantity = salesEntity.getQuantity();
            int price = salesEntity.getPrice();

            return Sales.builder()
                    .salesId(salesEntity.getId())
                    .productId(salesEntity.getProductsEntity().getId())
                    .productName(salesEntity.getProductsEntity().getName())
                    .quantity(quantity)
                    .price(price)
                    .totalPrice(price * quantity)
                    .createdAt(salesEntity.getCreatedAt())
                    .build();
        }
    }
}
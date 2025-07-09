package com.project.chaechaeserver.application.response.products;

import com.project.chaechaeserver.domain.model.products.ProductEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResGetProductDTO {

    private Product product;

    public static ResGetProductDTO from(ProductEntity productEntity) {
        return ResGetProductDTO.builder()
            .product(Product.from(productEntity))
            .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {

        private Long id;
        private String name;
        private String category;
        private Integer price;
        private String unit;
        private Integer currentQuantity;

        public static Product from(ProductEntity productEntity) {
            return Product.builder()
                .id(productEntity.getId())
                .name(productEntity.getName())
                .category(productEntity.getCategory())
                .price(productEntity.getPrice())
                .unit(productEntity.getUnit())
                .currentQuantity(productEntity.getQuantity())
                .build();
        }
    }
}
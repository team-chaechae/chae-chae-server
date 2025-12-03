package com.project.chaechaeserver.application.response.products;


import com.project.chaechaeserver.domain.model.products.ProductEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ResGetProductWithOrderStatus {

    private Product product;

    @Builder
    public ResGetProductWithOrderStatus(Product product) {
        this.product = product;
    }

    public static ResGetProductWithOrderStatus from(ProductEntity productEntity, Integer currentStock) {
        return ResGetProductWithOrderStatus.builder()
            .product(Product.from(productEntity, currentStock))
            .build();
    }

    @Getter
    @NoArgsConstructor
    public static class Product {

        private Long id;
        private String name;
        private String category;
        private Integer price;
        private Integer currentQuantity;
        private String status;
        private String orderStatus;

        @Builder
        public Product(Long id, String name, String category, Integer price, Integer currentQuantity, String status, String orderStatus) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.price = price;
            this.currentQuantity = currentQuantity;
            this.status = status;
            this.orderStatus = orderStatus;
        }

        public static Product from(ProductEntity productEntity, Integer currentStock) {
            return Product.builder()
                .id(productEntity.getId())
                .name(productEntity.getName())
                .category(productEntity.getCategory())
                .price(productEntity.getPrice())
                .currentQuantity(currentStock)
                .status(String.valueOf(productEntity.getProductStatusType()))
                .orderStatus(String.valueOf(productEntity.getOrderStatusType()))
                .build();
        }
    }

}
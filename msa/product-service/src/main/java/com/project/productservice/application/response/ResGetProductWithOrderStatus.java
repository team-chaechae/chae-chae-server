package com.project.productservice.application.response;


import com.project.productservice.application.service.ProductPriceSnapshot;
import com.project.productservice.domain.model.ProductEntity;
import com.project.productservice.domain.model.constraint.PromotionType;
import java.time.LocalDateTime;
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
        return from(productEntity, currentStock, ProductPriceSnapshot.original(productEntity));
    }

    public static ResGetProductWithOrderStatus from(ProductEntity productEntity, Integer currentStock,
        ProductPriceSnapshot priceSnapshot) {
        return ResGetProductWithOrderStatus.builder()
            .product(Product.from(productEntity, currentStock, priceSnapshot))
            .build();
    }

    @Getter
    @NoArgsConstructor
    public static class Product {

        private Long id;
        private String name;
        private String category;
        private Integer price;
        private Integer originalPrice;
        private boolean promotionApplied;
        private PromotionType promotionType;
        private Integer promotionPrice;
        private LocalDateTime promotionEndsAt;
        private Integer currentQuantity;
        private String status;
        private String orderStatus;

        @Builder
        public Product(Long id, String name, String category, Integer price, Integer originalPrice,
            boolean promotionApplied, PromotionType promotionType, Integer promotionPrice, LocalDateTime promotionEndsAt,
            Integer currentQuantity, String status, String orderStatus) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.price = price;
            this.originalPrice = originalPrice;
            this.promotionApplied = promotionApplied;
            this.promotionType = promotionType;
            this.promotionPrice = promotionPrice;
            this.promotionEndsAt = promotionEndsAt;
            this.currentQuantity = currentQuantity;
            this.status = status;
            this.orderStatus = orderStatus;
        }

        public static Product from(ProductEntity productEntity, Integer currentStock) {
            return from(productEntity, currentStock, ProductPriceSnapshot.original(productEntity));
        }

        public static Product from(ProductEntity productEntity, Integer currentStock,
            ProductPriceSnapshot priceSnapshot) {
            return Product.builder()
                .id(productEntity.getId())
                .name(productEntity.getName())
                .category(productEntity.getCategory())
                .price(priceSnapshot.getPrice())
                .originalPrice(priceSnapshot.getOriginalPrice())
                .promotionApplied(priceSnapshot.isPromotionApplied())
                .promotionType(priceSnapshot.getPromotionType())
                .promotionPrice(priceSnapshot.getPromotionPrice())
                .promotionEndsAt(priceSnapshot.getPromotionEndsAt())
                .currentQuantity(currentStock)
                .status(String.valueOf(productEntity.getProductStatusType()))
                .build();
        }
    }

}

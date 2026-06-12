package com.project.productservice.application.service;

import com.project.productservice.domain.model.ProductEntity;
import com.project.productservice.domain.model.PromotionEntity;
import com.project.productservice.domain.model.constraint.PromotionType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductPriceSnapshot {

    private Long productId;
    private Integer originalPrice;
    private Integer price;
    private boolean promotionApplied;
    private Long promotionId;
    private PromotionType promotionType;
    private Integer promotionPrice;
    private LocalDateTime promotionStartsAt;
    private LocalDateTime promotionEndsAt;

    public static ProductPriceSnapshot original(ProductEntity product) {
        return ProductPriceSnapshot.builder()
            .productId(product.getId())
            .originalPrice(product.getPrice())
            .price(product.getPrice())
            .promotionApplied(false)
            .build();
    }

    public static ProductPriceSnapshot promotion(ProductEntity product, PromotionEntity promotion) {
        return ProductPriceSnapshot.builder()
            .productId(product.getId())
            .originalPrice(promotion.getOriginalPrice())
            .price(promotion.getPromotionPrice())
            .promotionApplied(true)
            .promotionId(promotion.getId())
            .promotionType(promotion.getPromotionType())
            .promotionPrice(promotion.getPromotionPrice())
            .promotionStartsAt(promotion.getStartsAt())
            .promotionEndsAt(promotion.getEndsAt())
            .build();
    }
}

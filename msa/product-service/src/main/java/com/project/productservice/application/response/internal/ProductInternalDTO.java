package com.project.productservice.application.response.internal;

import com.project.productservice.domain.model.ProductEntity;
import com.project.productservice.application.service.ProductPriceSnapshot;
import com.project.productservice.domain.model.constraint.PromotionType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInternalDTO {

    private Long productId;
    private String name;
    private String category;
    private Integer price;
    private Integer originalPrice;
    private boolean promotionApplied;
    private PromotionType promotionType;
    private Integer promotionPrice;
    private LocalDateTime promotionEndsAt;

    public static ProductInternalDTO from(ProductEntity product) {
        return from(product, ProductPriceSnapshot.original(product));
    }

    public static ProductInternalDTO from(ProductEntity product, ProductPriceSnapshot priceSnapshot) {
        return ProductInternalDTO.builder()
            .productId(product.getId())
            .name(product.getName())
            .category(product.getCategory())
            .price(priceSnapshot.getPrice())
            .originalPrice(priceSnapshot.getOriginalPrice())
            .promotionApplied(priceSnapshot.isPromotionApplied())
            .promotionType(priceSnapshot.getPromotionType())
            .promotionPrice(priceSnapshot.getPromotionPrice())
            .promotionEndsAt(priceSnapshot.getPromotionEndsAt())
            .build();
    }
}

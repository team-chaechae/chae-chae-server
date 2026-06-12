package com.project.productservice.application.response;

import com.project.productservice.domain.model.PromotionEntity;
import com.project.productservice.domain.model.constraint.PromotionType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResPromotionDTO {

    private Long promotionId;
    private Long productId;
    private PromotionType promotionType;
    private Integer originalPrice;
    private Integer promotionPrice;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;

    public static ResPromotionDTO from(PromotionEntity promotion) {
        return ResPromotionDTO.builder()
            .promotionId(promotion.getId())
            .productId(promotion.getProductId())
            .promotionType(promotion.getPromotionType())
            .originalPrice(promotion.getOriginalPrice())
            .promotionPrice(promotion.getPromotionPrice())
            .startsAt(promotion.getStartsAt())
            .endsAt(promotion.getEndsAt())
            .build();
    }
}

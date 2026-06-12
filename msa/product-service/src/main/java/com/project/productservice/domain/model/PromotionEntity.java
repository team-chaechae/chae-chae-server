package com.project.productservice.domain.model;

import com.project.productservice.domain.model.constraint.PromotionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Table(
    name = "promotions",
    indexes = {
        @Index(name = "idx_promotions_product_period", columnList = "product_id, starts_at, ends_at"),
        @Index(name = "idx_promotions_type", columnList = "promotion_type"),
        @Index(name = "idx_promotions_cancelled_at", columnList = "cancelled_at")
    }
)
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PromotionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "promotion_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PromotionType promotionType;

    @Column(name = "original_price", nullable = false)
    private Integer originalPrice;

    @Column(name = "promotion_price", nullable = false)
    private Integer promotionPrice;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private PromotionEntity(Long productId, PromotionType promotionType, Integer originalPrice,
        Integer promotionPrice, LocalDateTime startsAt, LocalDateTime endsAt) {
        this.productId = productId;
        this.promotionType = promotionType;
        this.originalPrice = originalPrice;
        this.promotionPrice = promotionPrice;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public static PromotionEntity create(Long productId, PromotionType promotionType, Integer originalPrice,
        Integer promotionPrice, LocalDateTime startsAt, LocalDateTime endsAt) {
        return PromotionEntity.builder()
            .productId(productId)
            .promotionType(promotionType)
            .originalPrice(originalPrice)
            .promotionPrice(promotionPrice)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .build();
    }

    public void update(PromotionType promotionType, Integer originalPrice, Integer promotionPrice,
        LocalDateTime startsAt, LocalDateTime endsAt) {
        this.promotionType = promotionType;
        this.originalPrice = originalPrice;
        this.promotionPrice = promotionPrice;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public boolean isActiveAt(LocalDateTime now) {
        return cancelledAt == null
            && !now.isBefore(startsAt)
            && now.isBefore(endsAt);
    }
}

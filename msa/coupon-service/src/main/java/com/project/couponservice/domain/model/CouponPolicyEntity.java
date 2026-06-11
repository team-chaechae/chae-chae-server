package com.project.couponservice.domain.model;

import com.project.couponservice.application.global.exception.BadRequestException;
import com.project.couponservice.domain.model.constraint.CouponPolicyStatus;
import com.project.couponservice.domain.model.constraint.DiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(
        name = "coupon_policy",
        indexes = {
                @Index(name = "idx_coupon_policy_code", columnList = "code", unique = true),
                @Index(name = "idx_coupon_policy_status_period", columnList = "status, starts_at, ends_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private int discountValue;

    @Column(name = "min_order_amount", nullable = false)
    private int minOrderAmount;

    @Column(name = "max_discount_amount")
    private Integer maxDiscountAmount;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Column(name = "issued_quantity", nullable = false)
    private int issuedQuantity;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponPolicyStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private CouponPolicyEntity(
            String name,
            String code,
            DiscountType discountType,
            int discountValue,
            int minOrderAmount,
            Integer maxDiscountAmount,
            Integer totalQuantity,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            CouponPolicyStatus status
    ) {
        validate(name, code, discountType, discountValue, minOrderAmount, maxDiscountAmount, totalQuantity, startsAt, endsAt);
        this.name = name;
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.totalQuantity = totalQuantity;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = status;
    }

    public static CouponPolicyEntity create(
            String name,
            String code,
            DiscountType discountType,
            int discountValue,
            int minOrderAmount,
            Integer maxDiscountAmount,
            Integer totalQuantity,
            LocalDateTime startsAt,
            LocalDateTime endsAt
    ) {
        return new CouponPolicyEntity(
                name,
                code,
                discountType,
                discountValue,
                minOrderAmount,
                maxDiscountAmount,
                totalQuantity,
                startsAt,
                endsAt,
                CouponPolicyStatus.ACTIVE
        );
    }

    public void issue(LocalDateTime now) {
        if (!isDownloadable(now)) {
            throw new BadRequestException("다운로드할 수 없는 쿠폰입니다.");
        }
        issuedQuantity++;
    }

    public boolean isDownloadable(LocalDateTime now) {
        if (!isUsableAt(now)) {
            return false;
        }
        return totalQuantity == null || issuedQuantity < totalQuantity;
    }

    public boolean isUsableAt(LocalDateTime now) {
        return status == CouponPolicyStatus.ACTIVE && !now.isBefore(startsAt) && !now.isAfter(endsAt);
    }

    public int calculateDiscount(int orderAmount) {
        if (orderAmount < minOrderAmount) {
            throw new BadRequestException("최소 주문 금액을 충족하지 못했습니다.");
        }

        int discountAmount = switch (discountType) {
            case FIXED -> discountValue;
            case PERCENT -> orderAmount * discountValue / 100;
        };

        if (maxDiscountAmount != null) {
            discountAmount = Math.min(discountAmount, maxDiscountAmount);
        }
        return Math.min(discountAmount, orderAmount);
    }

    private static void validate(
            String name,
            String code,
            DiscountType discountType,
            int discountValue,
            int minOrderAmount,
            Integer maxDiscountAmount,
            Integer totalQuantity,
            LocalDateTime startsAt,
            LocalDateTime endsAt
    ) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("쿠폰명은 필수입니다.");
        }
        if (code == null || code.isBlank()) {
            throw new BadRequestException("쿠폰 코드는 필수입니다.");
        }
        if (discountType == null) {
            throw new BadRequestException("할인 타입은 필수입니다.");
        }
        if (discountValue < 1) {
            throw new BadRequestException("할인 값은 1 이상이어야 합니다.");
        }
        if (discountType == DiscountType.PERCENT && discountValue > 100) {
            throw new BadRequestException("정률 할인은 100%를 초과할 수 없습니다.");
        }
        if (minOrderAmount < 0) {
            throw new BadRequestException("최소 주문 금액은 0 이상이어야 합니다.");
        }
        if (maxDiscountAmount != null && maxDiscountAmount < 1) {
            throw new BadRequestException("최대 할인 금액은 1 이상이어야 합니다.");
        }
        if (totalQuantity != null && totalQuantity < 1) {
            throw new BadRequestException("총 발급 수량은 1 이상이어야 합니다.");
        }
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new BadRequestException("쿠폰 시작 시간은 종료 시간보다 이전이어야 합니다.");
        }
    }
}

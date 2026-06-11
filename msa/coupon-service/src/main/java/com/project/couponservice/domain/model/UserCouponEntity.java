package com.project.couponservice.domain.model;

import com.project.couponservice.application.global.exception.BadRequestException;
import com.project.couponservice.domain.model.constraint.UserCouponStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "user_coupon",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_coupon_user_policy", columnNames = {"user_id", "coupon_policy_id"})
        },
        indexes = {
                @Index(name = "idx_user_coupon_user_status", columnList = "user_id, status"),
                @Index(name = "idx_user_coupon_policy", columnList = "coupon_policy_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_policy_id", nullable = false)
    private CouponPolicyEntity policy;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserCouponStatus status;

    @Column(name = "used_sales_id")
    private Long usedSalesId;

    @Column(name = "used_order_amount")
    private Integer usedOrderAmount;

    @Column(name = "discount_amount")
    private Integer discountAmount;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private UserCouponEntity(CouponPolicyEntity policy, Long userId, LocalDateTime issuedAt) {
        if (policy == null) {
            throw new BadRequestException("쿠폰 정책은 필수입니다.");
        }
        if (userId == null || userId < 1) {
            throw new BadRequestException("사용자 ID는 필수입니다.");
        }
        this.policy = policy;
        this.userId = userId;
        this.status = UserCouponStatus.ISSUED;
        this.issuedAt = issuedAt;
    }

    public static UserCouponEntity issue(CouponPolicyEntity policy, Long userId, LocalDateTime issuedAt) {
        return new UserCouponEntity(policy, userId, issuedAt);
    }

    public int validateUsable(Long requestUserId, int orderAmount, LocalDateTime now) {
        assertOwnedBy(requestUserId);
        if (status != UserCouponStatus.ISSUED) {
            throw new BadRequestException("사용할 수 없는 쿠폰 상태입니다.");
        }
        if (!policy.isUsableAt(now)) {
            throw new BadRequestException("쿠폰 사용 가능 기간이 아닙니다.");
        }
        return policy.calculateDiscount(orderAmount);
    }

    public int use(Long requestUserId, Long salesId, int orderAmount, LocalDateTime now) {
        if (salesId == null || salesId < 1) {
            throw new BadRequestException("판매 ID는 필수입니다.");
        }
        assertOwnedBy(requestUserId);
        if (status == UserCouponStatus.USED) {
            if (salesId.equals(usedSalesId)) {
                if (!Integer.valueOf(orderAmount).equals(usedOrderAmount)) {
                    throw new BadRequestException("이미 사용된 쿠폰의 주문 금액과 일치하지 않습니다.");
                }
                return discountAmount;
            }
            throw new BadRequestException("이미 다른 주문에 사용된 쿠폰입니다.");
        }
        int discountAmount = validateUsable(requestUserId, orderAmount, now);
        this.status = UserCouponStatus.USED;
        this.usedSalesId = salesId;
        this.usedOrderAmount = orderAmount;
        this.discountAmount = discountAmount;
        this.usedAt = now;
        return discountAmount;
    }

    private void assertOwnedBy(Long requestUserId) {
        if (!userId.equals(requestUserId)) {
            throw new BadRequestException("쿠폰 소유자가 아닙니다.");
        }
    }
}

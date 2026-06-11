package com.project.couponservice.application.global.dto;

import com.project.couponservice.domain.model.CouponPolicyEntity;
import com.project.couponservice.domain.model.constraint.CouponPolicyStatus;
import com.project.couponservice.domain.model.constraint.DiscountType;
import java.time.LocalDateTime;

public record ResCouponPolicyDTO(
        Long id,
        String name,
        String code,
        DiscountType discountType,
        int discountValue,
        int minOrderAmount,
        Integer maxDiscountAmount,
        Integer totalQuantity,
        int issuedQuantity,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        CouponPolicyStatus status
) {

    public static ResCouponPolicyDTO from(CouponPolicyEntity policy) {
        return new ResCouponPolicyDTO(
                policy.getId(),
                policy.getName(),
                policy.getCode(),
                policy.getDiscountType(),
                policy.getDiscountValue(),
                policy.getMinOrderAmount(),
                policy.getMaxDiscountAmount(),
                policy.getTotalQuantity(),
                policy.getIssuedQuantity(),
                policy.getStartsAt(),
                policy.getEndsAt(),
                policy.getStatus()
        );
    }
}

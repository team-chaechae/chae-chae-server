package com.project.couponservice.application.global.dto;

import com.project.couponservice.domain.model.UserCouponEntity;
import com.project.couponservice.domain.model.constraint.UserCouponStatus;
import java.time.LocalDateTime;

public record ResUserCouponDTO(
        Long id,
        Long userId,
        UserCouponStatus status,
        Long usedSalesId,
        LocalDateTime issuedAt,
        LocalDateTime usedAt,
        ResCouponPolicyDTO policy
) {

    public static ResUserCouponDTO from(UserCouponEntity userCoupon) {
        return new ResUserCouponDTO(
                userCoupon.getId(),
                userCoupon.getUserId(),
                userCoupon.getStatus(),
                userCoupon.getUsedSalesId(),
                userCoupon.getIssuedAt(),
                userCoupon.getUsedAt(),
                ResCouponPolicyDTO.from(userCoupon.getPolicy())
        );
    }
}

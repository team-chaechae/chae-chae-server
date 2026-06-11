package com.project.couponservice.application.global.dto;

public record ResCouponDiscountDTO(
        Long userCouponId,
        Long userId,
        int orderAmount,
        int discountAmount,
        int finalAmount
) {

    public static ResCouponDiscountDTO of(Long userCouponId, Long userId, int orderAmount, int discountAmount) {
        return new ResCouponDiscountDTO(userCouponId, userId, orderAmount, discountAmount, orderAmount - discountAmount);
    }
}

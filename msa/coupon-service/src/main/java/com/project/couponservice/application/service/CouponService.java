package com.project.couponservice.application.service;

import com.project.couponservice.application.global.dto.ResCouponDiscountDTO;
import com.project.couponservice.application.global.dto.ResCouponPolicyDTO;
import com.project.couponservice.application.global.dto.ResUserCouponDTO;
import com.project.couponservice.presentation.request.ReqCreateCouponPolicyDTO;
import com.project.couponservice.presentation.request.ReqDownloadCouponDTO;
import com.project.couponservice.presentation.request.ReqUseCouponDTO;
import com.project.couponservice.presentation.request.ReqValidateCouponDTO;
import java.util.List;

public interface CouponService {

    ResCouponPolicyDTO createPolicy(ReqCreateCouponPolicyDTO request);

    List<ResCouponPolicyDTO> getActivePolicies();

    ResUserCouponDTO downloadCoupon(Long policyId, ReqDownloadCouponDTO request);

    List<ResUserCouponDTO> getUserCoupons(Long userId);

    ResCouponDiscountDTO validateCoupon(Long userCouponId, ReqValidateCouponDTO request);

    ResCouponDiscountDTO useCoupon(Long userCouponId, ReqUseCouponDTO request);
}

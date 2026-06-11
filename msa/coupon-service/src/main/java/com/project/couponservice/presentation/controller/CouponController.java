package com.project.couponservice.presentation.controller;

import com.project.couponservice.application.global.dto.ResCouponDiscountDTO;
import com.project.couponservice.application.global.dto.ResCouponPolicyDTO;
import com.project.couponservice.application.global.dto.ResUserCouponDTO;
import com.project.couponservice.application.response.ResDTO;
import com.project.couponservice.application.service.CouponService;
import com.project.couponservice.presentation.request.ReqCreateCouponPolicyDTO;
import com.project.couponservice.presentation.request.ReqDownloadCouponDTO;
import com.project.couponservice.presentation.request.ReqUseCouponDTO;
import com.project.couponservice.presentation.request.ReqValidateCouponDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/policies")
    public ResponseEntity<ResDTO<ResCouponPolicyDTO>> createPolicy(
            @Valid @RequestBody ReqCreateCouponPolicyDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ResDTO.created(couponService.createPolicy(request)));
    }

    @GetMapping("/policies/active")
    public ResponseEntity<ResDTO<List<ResCouponPolicyDTO>>> getActivePolicies() {
        return ResponseEntity.ok(ResDTO.success(couponService.getActivePolicies()));
    }

    @PostMapping("/policies/{policyId}/download")
    public ResponseEntity<ResDTO<ResUserCouponDTO>> downloadCoupon(
            @PathVariable @Min(1) Long policyId,
            @Valid @RequestBody ReqDownloadCouponDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ResDTO.created(couponService.downloadCoupon(policyId, request)));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ResDTO<List<ResUserCouponDTO>>> getUserCoupons(@PathVariable @Min(1) Long userId) {
        return ResponseEntity.ok(ResDTO.success(couponService.getUserCoupons(userId)));
    }

    @PostMapping("/{userCouponId}/validate")
    public ResponseEntity<ResDTO<ResCouponDiscountDTO>> validateCoupon(
            @PathVariable @Min(1) Long userCouponId,
            @Valid @RequestBody ReqValidateCouponDTO request
    ) {
        return ResponseEntity.ok(ResDTO.success(couponService.validateCoupon(userCouponId, request)));
    }

    @PostMapping("/{userCouponId}/use")
    public ResponseEntity<ResDTO<ResCouponDiscountDTO>> useCoupon(
            @PathVariable @Min(1) Long userCouponId,
            @Valid @RequestBody ReqUseCouponDTO request
    ) {
        return ResponseEntity.ok(ResDTO.success(couponService.useCoupon(userCouponId, request)));
    }
}

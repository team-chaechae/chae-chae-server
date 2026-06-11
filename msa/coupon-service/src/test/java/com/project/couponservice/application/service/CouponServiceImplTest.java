package com.project.couponservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.couponservice.application.global.dto.ResCouponDiscountDTO;
import com.project.couponservice.application.global.dto.ResCouponPolicyDTO;
import com.project.couponservice.application.global.dto.ResUserCouponDTO;
import com.project.couponservice.application.global.exception.BadRequestException;
import com.project.couponservice.domain.model.UserCouponEntity;
import com.project.couponservice.domain.model.constraint.DiscountType;
import com.project.couponservice.domain.model.constraint.UserCouponStatus;
import com.project.couponservice.domain.repository.CouponPolicyRepository;
import com.project.couponservice.domain.repository.UserCouponRepository;
import com.project.couponservice.presentation.request.ReqCreateCouponPolicyDTO;
import com.project.couponservice.presentation.request.ReqDownloadCouponDTO;
import com.project.couponservice.presentation.request.ReqUseCouponDTO;
import com.project.couponservice.presentation.request.ReqValidateCouponDTO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CouponServiceImplTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        userCouponRepository.deleteAll();
        couponPolicyRepository.deleteAll();
    }

    @Test
    void downloadCouponIssuesUserCoupon() {
        ResCouponPolicyDTO policy = createFixedPolicy("WELCOME-1000", 1_000, null);

        ResUserCouponDTO downloadedCoupon = couponService.downloadCoupon(
                policy.id(),
                ReqDownloadCouponDTO.builder()
                        .userId(1L)
                        .build()
        );

        assertThat(downloadedCoupon.id()).isNotNull();
        assertThat(downloadedCoupon.userId()).isEqualTo(1L);
        assertThat(downloadedCoupon.status()).isEqualTo(UserCouponStatus.ISSUED);
        assertThat(couponPolicyRepository.findById(policy.id()).orElseThrow().getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    void downloadCouponRejectsDuplicateDownload() {
        ResCouponPolicyDTO policy = createFixedPolicy("ONLY-ONCE", 1_000, null);
        ReqDownloadCouponDTO request = ReqDownloadCouponDTO.builder()
                .userId(1L)
                .build();
        couponService.downloadCoupon(policy.id(), request);

        assertThatThrownBy(() -> couponService.downloadCoupon(policy.id(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이미 다운로드한 쿠폰입니다.");
    }

    @Test
    void useCouponAppliesFixedDiscountAndMarksCouponUsed() {
        ResCouponPolicyDTO policy = createFixedPolicy("FIXED-3000", 3_000, null);
        ResUserCouponDTO downloadedCoupon = couponService.downloadCoupon(
                policy.id(),
                ReqDownloadCouponDTO.builder()
                        .userId(7L)
                        .build()
        );

        ResCouponDiscountDTO discount = couponService.useCoupon(
                downloadedCoupon.id(),
                ReqUseCouponDTO.builder()
                        .userId(7L)
                        .salesId(30L)
                        .orderAmount(10_000)
                        .build()
        );

        assertThat(discount.discountAmount()).isEqualTo(3_000);
        assertThat(discount.finalAmount()).isEqualTo(7_000);
        UserCouponEntity usedCoupon = userCouponRepository.findById(downloadedCoupon.id()).orElseThrow();
        assertThat(usedCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);
        assertThat(usedCoupon.getUsedSalesId()).isEqualTo(30L);
    }

    @Test
    void useCouponIsIdempotentForSameSalesId() {
        ResCouponPolicyDTO policy = createFixedPolicy("IDEMPOTENT-1000", 1_000, null);
        ResUserCouponDTO downloadedCoupon = couponService.downloadCoupon(
                policy.id(),
                ReqDownloadCouponDTO.builder()
                        .userId(7L)
                        .build()
        );
        ReqUseCouponDTO request = ReqUseCouponDTO.builder()
                .userId(7L)
                .salesId(30L)
                .orderAmount(10_000)
                .build();

        couponService.useCoupon(downloadedCoupon.id(), request);
        ResCouponDiscountDTO retriedDiscount = couponService.useCoupon(downloadedCoupon.id(), request);

        assertThat(retriedDiscount.discountAmount()).isEqualTo(1_000);
        assertThat(retriedDiscount.finalAmount()).isEqualTo(9_000);
    }

    @Test
    void useCouponRejectsRetryWithDifferentOrderAmount() {
        ResCouponPolicyDTO policy = createFixedPolicy("IDEMPOTENT-ORDER-AMOUNT", 1_000, null);
        ResUserCouponDTO downloadedCoupon = couponService.downloadCoupon(
                policy.id(),
                ReqDownloadCouponDTO.builder()
                        .userId(7L)
                        .build()
        );
        couponService.useCoupon(downloadedCoupon.id(), ReqUseCouponDTO.builder()
                .userId(7L)
                .salesId(30L)
                .orderAmount(10_000)
                .build());

        assertThatThrownBy(() -> couponService.useCoupon(downloadedCoupon.id(), ReqUseCouponDTO.builder()
                .userId(7L)
                .salesId(30L)
                .orderAmount(20_000)
                .build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이미 사용된 쿠폰의 주문 금액과 일치하지 않습니다.");
    }

    @Test
    void validateCouponCapsPercentDiscount() {
        ResCouponPolicyDTO policy = couponService.createPolicy(ReqCreateCouponPolicyDTO.builder()
                .name("20 percent")
                .code("PERCENT-20")
                .discountType(DiscountType.PERCENT)
                .discountValue(20)
                .minOrderAmount(1_000)
                .maxDiscountAmount(5_000)
                .totalQuantity(10)
                .startsAt(LocalDateTime.now().minusDays(1))
                .endsAt(LocalDateTime.now().plusDays(1))
                .build());
        ResUserCouponDTO downloadedCoupon = couponService.downloadCoupon(
                policy.id(),
                ReqDownloadCouponDTO.builder()
                        .userId(8L)
                        .build()
        );

        ResCouponDiscountDTO discount = couponService.validateCoupon(
                downloadedCoupon.id(),
                ReqValidateCouponDTO.builder()
                        .userId(8L)
                        .orderAmount(100_000)
                        .build()
        );

        assertThat(discount.discountAmount()).isEqualTo(5_000);
        assertThat(discount.finalAmount()).isEqualTo(95_000);
    }

    private ResCouponPolicyDTO createFixedPolicy(String code, int discountValue, Integer totalQuantity) {
        return couponService.createPolicy(ReqCreateCouponPolicyDTO.builder()
                .name(code)
                .code(code)
                .discountType(DiscountType.FIXED)
                .discountValue(discountValue)
                .minOrderAmount(1_000)
                .totalQuantity(totalQuantity)
                .startsAt(LocalDateTime.now().minusDays(1))
                .endsAt(LocalDateTime.now().plusDays(1))
                .build());
    }
}

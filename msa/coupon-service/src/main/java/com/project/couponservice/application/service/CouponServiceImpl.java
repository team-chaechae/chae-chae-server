package com.project.couponservice.application.service;

import com.project.couponservice.application.global.dto.ResCouponDiscountDTO;
import com.project.couponservice.application.global.dto.ResCouponPolicyDTO;
import com.project.couponservice.application.global.dto.ResUserCouponDTO;
import com.project.couponservice.application.global.exception.BadRequestException;
import com.project.couponservice.application.global.exception.EntityNotFoundException;
import com.project.couponservice.domain.model.CouponPolicyEntity;
import com.project.couponservice.domain.model.UserCouponEntity;
import com.project.couponservice.domain.model.constraint.CouponPolicyStatus;
import com.project.couponservice.domain.repository.CouponPolicyRepository;
import com.project.couponservice.domain.repository.UserCouponRepository;
import com.project.couponservice.presentation.request.ReqCreateCouponPolicyDTO;
import com.project.couponservice.presentation.request.ReqDownloadCouponDTO;
import com.project.couponservice.presentation.request.ReqUseCouponDTO;
import com.project.couponservice.presentation.request.ReqValidateCouponDTO;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponPolicyRepository couponPolicyRepository;
    private final UserCouponRepository userCouponRepository;
    private final Clock clock;

    @Override
    @Transactional
    public ResCouponPolicyDTO createPolicy(ReqCreateCouponPolicyDTO request) {
        if (couponPolicyRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("이미 존재하는 쿠폰 코드입니다.");
        }

        CouponPolicyEntity policy = CouponPolicyEntity.create(
                request.getName(),
                request.getCode(),
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getMinOrderAmount(),
                request.getMaxDiscountAmount(),
                request.getTotalQuantity(),
                request.getStartsAt(),
                request.getEndsAt()
        );

        CouponPolicyEntity savedPolicy = couponPolicyRepository.save(policy);
        log.info("coupon_policy_created policyId={} code={}", savedPolicy.getId(), savedPolicy.getCode());
        return ResCouponPolicyDTO.from(savedPolicy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResCouponPolicyDTO> getActivePolicies() {
        LocalDateTime now = LocalDateTime.now(clock);
        return couponPolicyRepository.findByStatusOrderByStartsAtDesc(CouponPolicyStatus.ACTIVE).stream()
                .filter(policy -> policy.isDownloadable(now))
                .map(ResCouponPolicyDTO::from)
                .toList();
    }

    @Override
    @Transactional
    public ResUserCouponDTO downloadCoupon(Long policyId, ReqDownloadCouponDTO request) {
        CouponPolicyEntity policy = couponPolicyRepository.findByIdForUpdate(policyId)
                .orElseThrow(() -> new EntityNotFoundException("쿠폰 정책을 찾을 수 없습니다."));

        if (userCouponRepository.existsByUserIdAndPolicyId(request.getUserId(), policyId)) {
            throw new BadRequestException("이미 다운로드한 쿠폰입니다.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        policy.issue(now);
        UserCouponEntity userCoupon = userCouponRepository.save(UserCouponEntity.issue(policy, request.getUserId(), now));
        log.info("coupon_downloaded policyId={} userCouponId={} userId={}", policyId, userCoupon.getId(), request.getUserId());
        return ResUserCouponDTO.from(userCoupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResUserCouponDTO> getUserCoupons(Long userId) {
        return userCouponRepository.findByUserIdOrderByIssuedAtDesc(userId).stream()
                .map(ResUserCouponDTO::from)
                .toList();
    }

    @Override
    @Transactional
    public ResCouponDiscountDTO validateCoupon(Long userCouponId, ReqValidateCouponDTO request) {
        UserCouponEntity userCoupon = userCouponRepository.findByIdWithPolicyForUpdate(userCouponId)
                .orElseThrow(() -> new EntityNotFoundException("사용자 쿠폰을 찾을 수 없습니다."));

        int discountAmount = userCoupon.validateUsable(request.getUserId(), request.getOrderAmount(), LocalDateTime.now(clock));
        return ResCouponDiscountDTO.of(userCoupon.getId(), request.getUserId(), request.getOrderAmount(), discountAmount);
    }

    @Override
    @Transactional
    public ResCouponDiscountDTO useCoupon(Long userCouponId, ReqUseCouponDTO request) {
        UserCouponEntity userCoupon = userCouponRepository.findByIdWithPolicyForUpdate(userCouponId)
                .orElseThrow(() -> new EntityNotFoundException("사용자 쿠폰을 찾을 수 없습니다."));

        int discountAmount = userCoupon.use(
                request.getUserId(),
                request.getSalesId(),
                request.getOrderAmount(),
                LocalDateTime.now(clock)
        );
        log.info(
                "coupon_used userCouponId={} userId={} salesId={} discountAmount={}",
                userCouponId,
                request.getUserId(),
                request.getSalesId(),
                discountAmount
        );
        return ResCouponDiscountDTO.of(userCoupon.getId(), request.getUserId(), request.getOrderAmount(), discountAmount);
    }
}

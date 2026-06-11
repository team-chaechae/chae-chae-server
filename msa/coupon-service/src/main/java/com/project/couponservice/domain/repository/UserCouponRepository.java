package com.project.couponservice.domain.repository;

import com.project.couponservice.domain.model.UserCouponEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCouponRepository extends JpaRepository<UserCouponEntity, Long> {

    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);

    @Query("""
            select userCoupon
            from UserCouponEntity userCoupon
            join fetch userCoupon.policy
            where userCoupon.userId = :userId
            order by userCoupon.issuedAt desc
            """)
    List<UserCouponEntity> findByUserIdOrderByIssuedAtDesc(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select userCoupon
            from UserCouponEntity userCoupon
            join fetch userCoupon.policy
            where userCoupon.id = :userCouponId
            """)
    Optional<UserCouponEntity> findByIdWithPolicyForUpdate(@Param("userCouponId") Long userCouponId);
}

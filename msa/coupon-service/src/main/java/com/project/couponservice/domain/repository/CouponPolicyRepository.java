package com.project.couponservice.domain.repository;

import com.project.couponservice.domain.model.CouponPolicyEntity;
import com.project.couponservice.domain.model.constraint.CouponPolicyStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponPolicyRepository extends JpaRepository<CouponPolicyEntity, Long> {

    boolean existsByCode(String code);

    List<CouponPolicyEntity> findByStatusOrderByStartsAtDesc(CouponPolicyStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select policy from CouponPolicyEntity policy where policy.id = :policyId")
    Optional<CouponPolicyEntity> findByIdForUpdate(@Param("policyId") Long policyId);
}

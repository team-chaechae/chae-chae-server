package com.project.productservice.infrastructure;

import com.project.productservice.domain.model.PromotionEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPromotionRepository extends JpaRepository<PromotionEntity, Long> {

    @Query("""
        select count(p) > 0
        from PromotionEntity p
        where p.productId = :productId
          and p.cancelledAt is null
          and p.startsAt < :endsAt
          and p.endsAt > :startsAt
        """)
    boolean existsOverlapping(
        @Param("productId") Long productId,
        @Param("startsAt") LocalDateTime startsAt,
        @Param("endsAt") LocalDateTime endsAt
    );

    Optional<PromotionEntity> findFirstByProductIdAndCancelledAtIsNullAndStartsAtLessThanEqualAndEndsAtAfterOrderByStartsAtDescIdDesc(
        Long productId,
        LocalDateTime startsAt,
        LocalDateTime endsAt
    );

    Optional<PromotionEntity> findFirstByProductIdAndCancelledAtIsNullOrderByCreatedAtDescIdDesc(Long productId);

    List<PromotionEntity> findByProductIdInAndCancelledAtIsNullAndStartsAtLessThanEqualAndEndsAtAfter(
        List<Long> productIds,
        LocalDateTime startsAt,
        LocalDateTime endsAt
    );
}

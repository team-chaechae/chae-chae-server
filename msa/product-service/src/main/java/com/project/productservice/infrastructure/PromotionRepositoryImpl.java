package com.project.productservice.infrastructure;

import com.project.productservice.domain.model.PromotionEntity;
import com.project.productservice.domain.repository.PromotionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PromotionRepositoryImpl implements PromotionRepository {

    private final JpaPromotionRepository jpaPromotionRepository;

    @Override
    public PromotionEntity save(PromotionEntity promotion) {
        return jpaPromotionRepository.save(promotion);
    }

    @Override
    public boolean existsOverlapping(Long productId, LocalDateTime startsAt, LocalDateTime endsAt) {
        return jpaPromotionRepository.existsOverlapping(productId, startsAt, endsAt);
    }

    @Override
    public Optional<PromotionEntity> findRegisteredByProductId(Long productId) {
        return jpaPromotionRepository.findFirstByProductIdAndCancelledAtIsNullOrderByCreatedAtDescIdDesc(productId);
    }

    @Override
    public Optional<PromotionEntity> findActiveByProductId(Long productId, LocalDateTime now) {
        return jpaPromotionRepository
            .findFirstByProductIdAndCancelledAtIsNullAndStartsAtLessThanEqualAndEndsAtAfterOrderByStartsAtDescIdDesc(
                productId,
                now,
                now
            );
    }

    @Override
    public List<PromotionEntity> findActiveByProductIds(List<Long> productIds, LocalDateTime now) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        return jpaPromotionRepository
            .findByProductIdInAndCancelledAtIsNullAndStartsAtLessThanEqualAndEndsAtAfter(
                productIds,
                now,
                now
            );
    }
}

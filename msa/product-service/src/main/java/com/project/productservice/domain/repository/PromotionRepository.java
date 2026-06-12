package com.project.productservice.domain.repository;

import com.project.productservice.domain.model.PromotionEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository {

    PromotionEntity save(PromotionEntity promotion);

    boolean existsOverlapping(Long productId, LocalDateTime startsAt, LocalDateTime endsAt);

    Optional<PromotionEntity> findRegisteredByProductId(Long productId);

    Optional<PromotionEntity> findActiveByProductId(Long productId, LocalDateTime now);

    List<PromotionEntity> findActiveByProductIds(List<Long> productIds, LocalDateTime now);
}

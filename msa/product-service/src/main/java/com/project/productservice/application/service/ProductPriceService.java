package com.project.productservice.application.service;

import com.project.productservice.domain.model.ProductEntity;
import com.project.productservice.domain.model.PromotionEntity;
import com.project.productservice.domain.repository.PromotionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductPriceService {

    private final PromotionRepository promotionRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ProductPriceSnapshot resolve(ProductEntity product) {
        LocalDateTime now = LocalDateTime.now(clock);
        return promotionRepository.findActiveByProductId(product.getId(), now)
            .map(promotion -> ProductPriceSnapshot.promotion(product, promotion))
            .orElseGet(() -> ProductPriceSnapshot.original(product));
    }

    @Transactional(readOnly = true)
    public Map<Long, ProductPriceSnapshot> resolveAll(List<ProductEntity> products) {
        if (products.isEmpty()) {
            return Map.of();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> productIds = products.stream()
            .map(ProductEntity::getId)
            .toList();
        Map<Long, PromotionEntity> activePromotions = promotionRepository.findActiveByProductIds(productIds, now)
            .stream()
            .collect(Collectors.toMap(
                PromotionEntity::getProductId,
                Function.identity(),
                this::pickLatestPromotion
            ));

        return products.stream()
            .collect(Collectors.toMap(
                ProductEntity::getId,
                product -> {
                    PromotionEntity promotion = activePromotions.get(product.getId());
                    if (promotion == null) {
                        return ProductPriceSnapshot.original(product);
                    }
                    return ProductPriceSnapshot.promotion(product, promotion);
                }
            ));
    }

    private PromotionEntity pickLatestPromotion(PromotionEntity first, PromotionEntity second) {
        return Comparator.comparing(PromotionEntity::getStartsAt)
            .thenComparing(promotion -> promotion.getId() == null ? 0L : promotion.getId())
            .compare(first, second) >= 0 ? first : second;
    }
}

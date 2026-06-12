package com.project.productservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.project.productservice.domain.model.PromotionEntity;
import com.project.productservice.domain.model.ProductEntity;
import com.project.productservice.domain.model.constraint.PromotionType;
import com.project.productservice.domain.repository.PromotionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductPriceServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2026-06-12T03:00:00Z"),
        ZoneId.of("Asia/Seoul")
    );

    @Mock
    private PromotionRepository promotionRepository;

    @Test
    void resolve_returnsPromotionPrice_whenActivePromotionExists() {
        ProductPriceService productPriceService = new ProductPriceService(promotionRepository, FIXED_CLOCK);
        ProductEntity product = product(1L, 12_000);
        LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
        PromotionEntity promotion = PromotionEntity.create(
            1L,
            PromotionType.HOT_DEAL,
            10_000,
            7_000,
            now.minusMinutes(10),
            now.plusMinutes(10)
        );
        when(promotionRepository.findActiveByProductId(1L, now)).thenReturn(Optional.of(promotion));

        ProductPriceSnapshot snapshot = productPriceService.resolve(product);

        assertThat(snapshot.getPrice()).isEqualTo(7_000);
        assertThat(snapshot.getOriginalPrice()).isEqualTo(10_000);
        assertThat(snapshot.isPromotionApplied()).isTrue();
        assertThat(snapshot.getPromotionType()).isEqualTo(PromotionType.HOT_DEAL);
        assertThat(snapshot.getPromotionPrice()).isEqualTo(7_000);
        assertThat(snapshot.getPromotionEndsAt()).isEqualTo(now.plusMinutes(10));
    }

    @Test
    void resolve_returnsOriginalPrice_whenActivePromotionDoesNotExist() {
        ProductPriceService productPriceService = new ProductPriceService(promotionRepository, FIXED_CLOCK);
        ProductEntity product = product(1L, 10_000);
        LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
        when(promotionRepository.findActiveByProductId(1L, now)).thenReturn(Optional.empty());

        ProductPriceSnapshot snapshot = productPriceService.resolve(product);

        assertThat(snapshot.getPrice()).isEqualTo(10_000);
        assertThat(snapshot.getOriginalPrice()).isEqualTo(10_000);
        assertThat(snapshot.isPromotionApplied()).isFalse();
        assertThat(snapshot.getPromotionPrice()).isNull();
        assertThat(snapshot.getPromotionEndsAt()).isNull();
    }

    private ProductEntity product(Long productId, Integer price) {
        ProductEntity product = ProductEntity.createProducts("test-product-" + productId, "test", price);
        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }
}

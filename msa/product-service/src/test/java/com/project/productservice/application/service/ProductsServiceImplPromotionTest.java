package com.project.productservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.productservice.application.global.exception.BadRequestException;
import com.project.productservice.application.response.ResPromotionDTO;
import com.project.productservice.domain.model.ProductEntity;
import com.project.productservice.domain.model.PromotionEntity;
import com.project.productservice.domain.model.constraint.PromotionType;
import com.project.productservice.domain.repository.ProductsRepository;
import com.project.productservice.domain.repository.PromotionRepository;
import com.project.productservice.infrastructure.client.InventoryClient;
import com.project.productservice.presentation.request.ReqCreatePromotionDTO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductsServiceImplPromotionTest {

    @Mock
    private ProductsRepository productsRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private ProductCacheService productCacheService;

    @Mock
    private ProductPriceService productPriceService;

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductsServiceImpl productsService;

    @Test
    void createPromotion_savesTimeSalePromotionAndEvictsProductCaches() {
        ProductEntity product = product(1L, 10_000);
        LocalDateTime startsAt = LocalDateTime.of(2026, 6, 12, 12, 0);
        LocalDateTime endsAt = startsAt.plusHours(2);
        ReqCreatePromotionDTO request = new ReqCreatePromotionDTO(PromotionType.TIME_SALE, 7_000, startsAt, endsAt);
        when(productsRepository.findByIdForUpdate(1L)).thenReturn(product);
        when(promotionRepository.existsOverlapping(1L, startsAt, endsAt)).thenReturn(false);
        when(promotionRepository.save(any(PromotionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResPromotionDTO response = productsService.createPromotion(1L, request);

        assertThat(response.getProductId()).isEqualTo(1L);
        assertThat(product.isPromo()).isTrue();
        assertThat(response.getPromotionType()).isEqualTo(PromotionType.TIME_SALE);
        assertThat(response.getOriginalPrice()).isEqualTo(10_000);
        assertThat(response.getPromotionPrice()).isEqualTo(7_000);
        assertThat(response.getStartsAt()).isEqualTo(startsAt);
        assertThat(response.getEndsAt()).isEqualTo(endsAt);
        verify(productCacheService).evictProductCache(1L);
        verify(productCacheService).evictProductInternalCache(1L);
        verify(productCacheService).evictAllProductListCache();
    }

    @Test
    void createPromotion_savesHotDealPromotion() {
        ProductEntity product = product(1L, 10_000);
        LocalDateTime startsAt = LocalDateTime.of(2026, 6, 12, 12, 0);
        LocalDateTime endsAt = startsAt.plusHours(2);
        ReqCreatePromotionDTO request = new ReqCreatePromotionDTO(PromotionType.HOT_DEAL, 6_000, startsAt, endsAt);
        when(productsRepository.findByIdForUpdate(1L)).thenReturn(product);
        when(promotionRepository.existsOverlapping(1L, startsAt, endsAt)).thenReturn(false);
        when(promotionRepository.save(any(PromotionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResPromotionDTO response = productsService.createPromotion(1L, request);

        assertThat(response.getPromotionType()).isEqualTo(PromotionType.HOT_DEAL);
        assertThat(response.getPromotionPrice()).isEqualTo(6_000);
    }

    @Test
    void createPromotion_throwsBadRequest_whenProductAlreadyHasPromotion() {
        ProductEntity product = product(1L, 10_000);
        product.markPromo();
        LocalDateTime startsAt = LocalDateTime.of(2026, 6, 12, 12, 0);
        LocalDateTime endsAt = startsAt.plusHours(2);
        ReqCreatePromotionDTO request = new ReqCreatePromotionDTO(PromotionType.TIME_SALE, 7_000, startsAt, endsAt);
        when(productsRepository.findByIdForUpdate(1L)).thenReturn(product);

        assertThatThrownBy(() -> productsService.createPromotion(1L, request))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("이미 프로모션이 적용된 상품입니다. 프로모션 수정 API를 사용해주세요.");

        verify(promotionRepository, never()).save(any(PromotionEntity.class));
    }

    @Test
    void updatePromotion_updatesRegisteredPromotion() {
        ProductEntity product = product(1L, 10_000);
        product.markPromo();
        LocalDateTime startsAt = LocalDateTime.of(2026, 6, 12, 12, 0);
        LocalDateTime endsAt = startsAt.plusHours(2);
        PromotionEntity promotion = PromotionEntity.create(
            1L,
            PromotionType.TIME_SALE,
            10_000,
            7_000,
            startsAt.minusDays(1),
            endsAt.minusDays(1)
        );
        ReqCreatePromotionDTO request = new ReqCreatePromotionDTO(PromotionType.HOT_DEAL, 6_000, startsAt, endsAt);
        when(productsRepository.findByIdForUpdate(1L)).thenReturn(product);
        when(promotionRepository.findRegisteredByProductId(1L)).thenReturn(java.util.Optional.of(promotion));

        ResPromotionDTO response = productsService.updatePromotion(1L, request);

        assertThat(response.getProductId()).isEqualTo(1L);
        assertThat(response.getPromotionType()).isEqualTo(PromotionType.HOT_DEAL);
        assertThat(response.getPromotionPrice()).isEqualTo(6_000);
        assertThat(response.getStartsAt()).isEqualTo(startsAt);
        assertThat(response.getEndsAt()).isEqualTo(endsAt);
        verify(productCacheService).evictProductCache(1L);
        verify(productCacheService).evictProductInternalCache(1L);
        verify(productCacheService).evictAllProductListCache();
    }

    @Test
    void updatePromotion_throwsBadRequest_whenProductDoesNotHavePromotion() {
        ProductEntity product = product(1L, 10_000);
        LocalDateTime startsAt = LocalDateTime.of(2026, 6, 12, 12, 0);
        LocalDateTime endsAt = startsAt.plusHours(2);
        ReqCreatePromotionDTO request = new ReqCreatePromotionDTO(PromotionType.HOT_DEAL, 6_000, startsAt, endsAt);
        when(productsRepository.findByIdForUpdate(1L)).thenReturn(product);

        assertThatThrownBy(() -> productsService.updatePromotion(1L, request))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("적용된 프로모션이 없습니다. 프로모션 생성 API를 사용해주세요.");
    }

    @Test
    void createPromotion_throwsBadRequest_whenOverlappingPromotionExists() {
        ProductEntity product = product(1L, 10_000);
        LocalDateTime startsAt = LocalDateTime.of(2026, 6, 12, 12, 0);
        LocalDateTime endsAt = startsAt.plusHours(2);
        ReqCreatePromotionDTO request = new ReqCreatePromotionDTO(PromotionType.TIME_SALE, 7_000, startsAt, endsAt);
        when(productsRepository.findByIdForUpdate(1L)).thenReturn(product);
        when(promotionRepository.existsOverlapping(1L, startsAt, endsAt)).thenReturn(true);

        assertThatThrownBy(() -> productsService.createPromotion(1L, request))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("해당 기간에 이미 등록된 프로모션이 있습니다.");

        verify(promotionRepository, never()).save(any(PromotionEntity.class));
    }

    private ProductEntity product(Long productId, Integer price) {
        ProductEntity product = ProductEntity.createProducts("test-product-" + productId, "test", price);
        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }
}

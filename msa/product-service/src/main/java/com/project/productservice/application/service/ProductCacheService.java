package com.project.productservice.application.service;

import com.project.productservice.application.response.internal.ProductInternalDTO;
import com.project.productservice.domain.model.ProductEntity;
import com.project.productservice.domain.repository.ProductsRepository;
import com.project.productservice.infrastructure.config.TwoLevelCacheConfig;
import com.project.productservice.infrastructure.config.TwoLevelCacheService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 캐시 서비스
 * 2레벨 캐싱을 통한 상품 조회 최적화
 *
 * L1 (Caffeine): 1분 TTL, 로컬 메모리
 * L2 (Redis): 10분 TTL, 분산 캐시
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheService {

    private final TwoLevelCacheService cacheService;
    private final ProductsRepository productsRepository;
    private final ProductPriceService productPriceService;

    /**
     * 애플리케이션 시작 시 상품 캐시 상태 확인.
     * 내부 주문용 가격은 프로모션 시간 경계에서 바뀌므로 워밍업하지 않는다.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void warmUpCache() {
        log.info("[Cache Warmup] 상품 캐시 워밍업 시작...");
        long startTime = System.currentTimeMillis();

        try {
            List<ProductEntity> allProducts = productsRepository.findAll();
            log.info("[Cache Warmup] DB에서 {}개 상품 조회 완료", allProducts.size());

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[Cache Warmup] 완료 - 프로모션 가격 정확성을 위해 내부 주문용 가격 캐시는 워밍업하지 않음, 소요시간: {}ms", elapsed);
        } catch (Exception e) {
            log.error("[Cache Warmup] 실패 - {}", e.getMessage(), e);
        }
    }

    /**
     * 단일 상품 조회 (2레벨 캐싱 적용)
     */
    @Transactional(readOnly = true)
    public ProductEntity getProduct(Long productId) {
        return cacheService.get(
            TwoLevelCacheConfig.PRODUCT_CACHE,
            productId,
            ProductEntity.class,
            () -> productsRepository.findProductByProductId(productId)
        );
    }

    /**
     * 내부 서비스용 상품 조회.
     * 주문 가격 스냅샷은 현재 프로모션 가격을 반영해야 하므로 캐시하지 않는다.
     */
    @Transactional(readOnly = true)
    public ProductInternalDTO getProductInternal(Long productId) {
        ProductEntity product = productsRepository.findProductByProductId(productId);
        return ProductInternalDTO.from(product, productPriceService.resolve(product));
    }

    /**
     * 내부 서비스용 상품 배치 조회.
     * 동일 요청 안에서는 상품과 프로모션을 각각 한 번씩 배치 조회한다.
     */
    @Transactional(readOnly = true)
    public Map<Long, ProductInternalDTO> getProductsInternal(List<Long> productIds) {
        List<ProductEntity> products = productsRepository.findAllById(productIds);
        Map<Long, ProductPriceSnapshot> priceSnapshots = productPriceService.resolveAll(products);

        return products.stream()
            .map(product -> ProductInternalDTO.from(product, priceSnapshots.get(product.getId())))
            .collect(Collectors.toMap(
                ProductInternalDTO::getProductId,
                Function.identity()
            ));
    }

    /**
     * 상품 목록 조회 (2레벨 캐싱 적용)
     * 캐시 키: "list:{page}:{size}" (기본 목록만 캐싱)
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Page<ProductEntity> getProductList(Pageable pageable) {
        String cacheKey = String.format("list:%d:%d", pageable.getPageNumber(), pageable.getPageSize());

        return cacheService.get(
            TwoLevelCacheConfig.PRODUCT_LIST_CACHE,
            cacheKey,
            Page.class,
            () -> productsRepository.findProductByDeletedAtIsNullWithCondition(
                pageable, null, null, null, null, null, null, null
            )
        );
    }

    /**
     * 상품 캐시 무효화
     */
    public void evictProductCache(Long productId) {
        cacheService.evict(TwoLevelCacheConfig.PRODUCT_CACHE, productId);
        log.info("Product cache evicted for productId: {}", productId);
    }

    public void evictProductInternalCache(Long productId) {
        cacheService.evict(TwoLevelCacheConfig.PRODUCT_CACHE, "internal:" + productId);
        log.info("Product internal cache evicted for productId: {}", productId);
    }

    /**
     * 상품 목록 캐시 전체 무효화
     */
    public void evictAllProductListCache() {
        cacheService.evictAll(TwoLevelCacheConfig.PRODUCT_LIST_CACHE);
        log.info("All product list cache evicted");
    }

    /**
     * 전체 캐시 무효화
     */
    public void evictAllCache() {
        cacheService.evictAll(TwoLevelCacheConfig.PRODUCT_CACHE);
        cacheService.evictAll(TwoLevelCacheConfig.PRODUCT_LIST_CACHE);
        log.info("All product cache evicted");
    }
}

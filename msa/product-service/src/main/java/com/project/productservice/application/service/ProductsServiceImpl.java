package com.project.productservice.application.service;

import com.project.productservice.application.event.ProductCreatedInternalEvent;
import com.project.productservice.application.global.exception.BadRequestException;
import com.project.productservice.application.global.exception.EntityAlreadyExistException;
import com.project.productservice.application.response.ResCreateProductPostDTO;
import com.project.productservice.application.response.ResGetProductWithOrderStatus;
import com.project.productservice.application.response.ResPromotionDTO;
import com.project.productservice.application.response.ResProductSearchWithOrderStatusDTO;
import com.project.productservice.domain.model.ProductEntity;
import com.project.productservice.domain.model.PromotionEntity;
import com.project.productservice.domain.model.constraint.ProductStatusType;
import com.project.productservice.domain.repository.ProductsRepository;
import com.project.productservice.domain.repository.PromotionRepository;
import com.project.productservice.infrastructure.client.InventoryClient;
import com.project.productservice.presentation.request.ReqCreateProductsDTO;
import com.project.productservice.presentation.request.ReqCreatePromotionDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductsServiceImpl implements ProductsService {

    private final ProductsRepository productsRepository;
    private final InventoryClient inventoryClient;
    private final ProductCacheService productCacheService;
    private final ProductPriceService productPriceService;
    private final PromotionRepository promotionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ResCreateProductPostDTO createProductInfo(ReqCreateProductsDTO dto) {

        // 상품명 중복 검증
        if (productsRepository.existsByProductName(dto.getProduct().getName())) {
            throw new EntityAlreadyExistException("이미 존재하는 상품명입니다: " + dto.getProduct().getName());
        }

        // 1. 도메인 로직
        ProductEntity savedProduct = productsRepository.save(
            ProductEntity.createProducts(
                dto.getProduct().getName(),
                dto.getProduct().getCategory(),
                dto.getProduct().getPrice()
            )
        );

        // 2. 이벤트 발행 (BEFORE_COMMIT: Outbox 저장, AFTER_COMMIT: Kafka 발행)
        ProductCreatedInternalEvent event = ProductCreatedInternalEvent.of(
                savedProduct.getId(),
                savedProduct.getName(),
                savedProduct.getCategory(),
                savedProduct.getPrice()
        );
        eventPublisher.publishEvent(event);

        return ResCreateProductPostDTO.from(savedProduct);
    }

    @Override
    @Transactional
    public ResPromotionDTO createPromotion(Long productId, ReqCreatePromotionDTO request) {
        ProductEntity product = productsRepository.findByIdForUpdate(productId);
        if (product.isPromo()) {
            throw new BadRequestException("이미 프로모션이 적용된 상품입니다. 프로모션 수정 API를 사용해주세요.");
        }
        validatePromotion(product, request, true);

        PromotionEntity promotion = promotionRepository.save(
            PromotionEntity.create(
                product.getId(),
                request.getPromotionType(),
                product.getPrice(),
                request.getPromotionPrice(),
                request.getStartsAt(),
                request.getEndsAt()
            )
        );
        product.markPromo();

        evictPromotionCachesAfterCommit(productId);
        log.info("[프로모션 생성] productId: {}, promotionId: {}, promotionType: {}, originalPrice: {}, promotionPrice: {}, startsAt: {}, endsAt: {}",
            productId, promotion.getId(), request.getPromotionType(), product.getPrice(), request.getPromotionPrice(),
            request.getStartsAt(), request.getEndsAt());

        return ResPromotionDTO.from(promotion);
    }

    @Override
    @Transactional
    public ResPromotionDTO updatePromotion(Long productId, ReqCreatePromotionDTO request) {
        ProductEntity product = productsRepository.findByIdForUpdate(productId);
        if (!product.isPromo()) {
            throw new BadRequestException("적용된 프로모션이 없습니다. 프로모션 생성 API를 사용해주세요.");
        }
        validatePromotion(product, request, false);

        PromotionEntity promotion = promotionRepository.findRegisteredByProductId(productId)
            .orElseThrow(() -> new BadRequestException("적용된 프로모션 정보를 찾을 수 없습니다."));
        promotion.update(
            request.getPromotionType(),
            product.getPrice(),
            request.getPromotionPrice(),
            request.getStartsAt(),
            request.getEndsAt()
        );

        evictPromotionCachesAfterCommit(productId);
        log.info("[프로모션 수정] productId: {}, promotionId: {}, promotionType: {}, originalPrice: {}, promotionPrice: {}, startsAt: {}, endsAt: {}",
            productId, promotion.getId(), request.getPromotionType(), product.getPrice(), request.getPromotionPrice(),
            request.getStartsAt(), request.getEndsAt());

        return ResPromotionDTO.from(promotion);
    }

    @Override
    @Transactional(readOnly = true)
    public ResGetProductWithOrderStatus getProductInfo(Long productId) {

        ProductEntity product = productsRepository.findProductByProductId(productId);
        ProductPriceSnapshot priceSnapshot = productPriceService.resolve(product);

        Integer currentStock = inventoryClient.getCurrentStock(productId);

        return ResGetProductWithOrderStatus.from(product, currentStock, priceSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public ResProductSearchWithOrderStatusDTO getProductSearchInfo(Pageable pageable,
        String productName, Boolean deletedAt, ProductStatusType productStatus, LocalDate startDate,
        LocalDate endDate, LocalDate exactDate
        , List<String> sortList) {

        // 1. 상품 페이지 조회
        Page<ProductEntity> productPage = productsRepository.findProductByDeletedAtIsNullWithCondition(pageable,
             productName,  deletedAt,  productStatus,  startDate,
             endDate,  exactDate, sortList);

        // 2. 상품 ID 목록 추출
        List<Long> productIds = productPage.getContent().stream()
            .map(ProductEntity::getId)
            .toList();

        // 3. MSA: inventory-service에서 재고 조회
        Map<Long, Integer> stockMap = inventoryClient.getCurrentStockMap(productIds);
        Map<Long, ProductPriceSnapshot> priceSnapshotMap = productPriceService.resolveAll(productPage.getContent());

        // 4. DTO 변환 (재고 정보 포함)
        return ResProductSearchWithOrderStatusDTO.from(productPage, stockMap, priceSnapshotMap);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> validateProductIds(List<Long> productIds) {
        List<ProductEntity> foundProducts = productsRepository.findAllById(productIds);
        return foundProducts.stream()
            .map(ProductEntity::getId)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public com.project.productservice.application.response.internal.ProductInternalDTO getProductForInternal(Long productId) {
        // 경량 DTO 캐시 조회 (productId, name, category, price만)
        log.debug("Product internal cache lookup for productId: {}", productId);
        return productCacheService.getProductInternal(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, com.project.productservice.application.response.internal.ProductInternalDTO> getProductsForInternal(List<Long> productIds) {
        log.debug("Product internal batch cache lookup for {} products", productIds.size());
        return productCacheService.getProductsInternal(productIds);
    }

    private void validatePromotion(ProductEntity product, ReqCreatePromotionDTO request, boolean checkOverlap) {
        if (!request.getStartsAt().isBefore(request.getEndsAt())) {
            throw new BadRequestException("프로모션 시작 시각은 종료 시각보다 빨라야 합니다.");
        }
        if (request.getPromotionPrice() >= product.getPrice()) {
            throw new BadRequestException("프로모션 가격은 상품 정가보다 낮아야 합니다.");
        }
        if (checkOverlap && promotionRepository.existsOverlapping(product.getId(), request.getStartsAt(), request.getEndsAt())) {
            throw new BadRequestException("해당 기간에 이미 등록된 프로모션이 있습니다.");
        }
    }

    private void evictPromotionCachesAfterCommit(Long productId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            evictPromotionCaches(productId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evictPromotionCaches(productId);
            }
        });
    }

    private void evictPromotionCaches(Long productId) {
        try {
            productCacheService.evictProductCache(productId);
            productCacheService.evictProductInternalCache(productId);
            productCacheService.evictAllProductListCache();
        } catch (Exception e) {
            log.error("[프로모션 캐시 무효화 실패] productId: {}, error: {}", productId, e.getMessage(), e);
        }
    }

}

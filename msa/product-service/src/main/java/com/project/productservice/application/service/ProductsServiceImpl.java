package com.project.productservice.application.service;

import com.project.productservice.application.event.ProductCreatedInternalEvent;
import com.project.productservice.application.global.exception.EntityAlreadyExistException;
import com.project.productservice.application.response.ResCreateProductPostDTO;
import com.project.productservice.application.response.ResGetProductWithOrderStatus;
import com.project.productservice.application.response.ResProductSearchWithOrderStatusDTO;
import com.project.productservice.domain.model.ProductEntity;
import com.project.productservice.domain.model.constraint.ProductStatusType;
import com.project.productservice.domain.repository.ProductsRepository;
import com.project.productservice.infrastructure.client.InventoryClient;
import com.project.productservice.presentation.request.ReqCreateProductsDTO;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductsServiceImpl implements ProductsService {

    private final ProductsRepository productsRepository;
    private final InventoryClient inventoryClient;
    private final ProductCacheService productCacheService;
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
    @Transactional(readOnly = true)
    public ResGetProductWithOrderStatus getProductInfo(Long productId) {

        ProductEntity product = productsRepository.findProductByProductId(productId);

        Integer currentStock = inventoryClient.getCurrentStock(productId);

        return ResGetProductWithOrderStatus.from(product, currentStock);
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

        // 4. DTO 변환 (재고 정보 포함)
        return ResProductSearchWithOrderStatusDTO.from(productPage, stockMap);
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

        // 배치로 조회 (각각 캐시 적용)
        Map<Long, com.project.productservice.application.response.internal.ProductInternalDTO> result = new java.util.HashMap<>();
        for (Long productId : productIds) {
            result.put(productId, productCacheService.getProductInternal(productId));
        }
        return result;
    }

}

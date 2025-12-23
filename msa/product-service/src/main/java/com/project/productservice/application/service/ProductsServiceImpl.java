package com.project.productservice.application.service;

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

    @Override
    @Transactional
    public ResCreateProductPostDTO createProductInfo(ReqCreateProductsDTO dto) {

        // 상품명 중복 검증
        if (productsRepository.existsByProductName(dto.getProduct().getName())) {
            throw new EntityAlreadyExistException("이미 존재하는 상품명입니다: " + dto.getProduct().getName());
        }

        ProductEntity savedProduct = productsRepository.save(
            ProductEntity.createProducts(
                dto.getProduct().getName(),
                dto.getProduct().getCategory(),
                dto.getProduct().getPrice()
            )
        );

        return ResCreateProductPostDTO.from(
        savedProduct
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResGetProductWithOrderStatus getProductInfo(Long productId) {

        ProductEntity product = productsRepository.findProductByProductId(productId);

        // MSA: inventory-service에서 재고 조회
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
        ProductEntity product = productsRepository.findProductByProductId(productId);
        return com.project.productservice.application.response.internal.ProductInternalDTO.from(product);
    }

}

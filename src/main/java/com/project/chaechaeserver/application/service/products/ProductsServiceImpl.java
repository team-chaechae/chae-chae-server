package com.project.chaechaeserver.application.service.products;

import com.project.chaechaeserver.application.global.excepion.EntityAlreadyExistException;
import com.project.chaechaeserver.application.response.products.ResCreateProductPostDTO;
import com.project.chaechaeserver.application.response.products.ResGetProductWithOrderStatus;
import com.project.chaechaeserver.application.response.products.ResProductSearchWithOrderStatusDTO;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import com.project.chaechaeserver.domain.repository.inventory.InventoryRepository;
import com.project.chaechaeserver.domain.repository.order.OrderRepository;
import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
import com.project.chaechaeserver.presentation.request.products.ReqCreateProductsDTO;
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
    private final InventoryRepository inventoryRepository;

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

        // 히스토리 기반 재고 조회
        Integer currentStock = inventoryRepository.getCurrentStock(productId);

        return ResGetProductWithOrderStatus.from(product, currentStock);
    }

    @Override
    @Transactional(readOnly = true)
    public ResProductSearchWithOrderStatusDTO getProductSearchInfo(Pageable pageable,
        String productName, Boolean deletedAt, ProductStatusType productStatus,ProductStatusType.ProductOrderType orderStatus , LocalDate startDate,
        LocalDate endDate, LocalDate exactDate
        , List<String> sortList) {

        // 1. 상품 페이지 조회
        Page<ProductEntity> productPage = productsRepository.findProductByDeletedAtIsNullWithCondition(pageable,
             productName,  deletedAt,  productStatus, orderStatus,  startDate,
             endDate,  exactDate, sortList);

        // 2. 상품 ID 목록 추출
        List<Long> productIds = productPage.getContent().stream()
            .map(ProductEntity::getId)
            .toList();

        // 3. 히스토리 기반 재고 조회
        Map<Long, Integer> stockMap = inventoryRepository.getCurrentStockMap(productIds);

        // 4. DTO 변환 (재고 정보 포함)
        return ResProductSearchWithOrderStatusDTO.from(productPage, stockMap);
    }

}


package com.project.productservice.application.service;


import com.project.productservice.application.response.ResCreateProductPostDTO;
import com.project.productservice.application.response.ResGetProductWithOrderStatus;
import com.project.productservice.application.response.ResPromotionDTO;
import com.project.productservice.application.response.ResProductSearchWithOrderStatusDTO;
import com.project.productservice.domain.model.constraint.ProductStatusType;
import com.project.productservice.presentation.request.ReqCreateProductsDTO;
import com.project.productservice.presentation.request.ReqCreatePromotionDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ProductsService {

    ResCreateProductPostDTO createProductInfo(ReqCreateProductsDTO request);
    ResPromotionDTO createPromotion(Long productId, ReqCreatePromotionDTO request);
    ResPromotionDTO updatePromotion(Long productId, ReqCreatePromotionDTO request);
    ResGetProductWithOrderStatus getProductInfo(Long productId);
    ResProductSearchWithOrderStatusDTO getProductSearchInfo(
        Pageable pageable,
        String productName, Boolean deletedAt, ProductStatusType productStatus, LocalDate startDate,
        LocalDate endDate, LocalDate exactDate
        , List<String> sortList);

    /**
     * 상품 ID 목록의 유효성 검증 (inventory-service에서 호출)
     * @param productIds 검증할 상품 ID 목록
     * @return 유효한 상품 ID 목록
     */
    List<Long> validateProductIds(List<Long> productIds);

    /**
     * 내부 서비스용 상품 정보 조회 (order-service에서 호출)
     * @param productId 상품 ID
     * @return 상품 정보 DTO
     */
    com.project.productservice.application.response.internal.ProductInternalDTO getProductForInternal(Long productId);

    /**
     * 내부 서비스용 상품 정보 배치 조회 (order-service에서 호출)
     * @param productIds 상품 ID 목록
     * @return 상품 ID -> 상품 정보 Map
     */
    java.util.Map<Long, com.project.productservice.application.response.internal.ProductInternalDTO> getProductsForInternal(List<Long> productIds);
}

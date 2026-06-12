package com.project.orderservice.infrastructure.client;

import com.project.orderservice.infrastructure.client.dto.ProductDTO;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 상품 조회 클라이언트.
 * 프로모션 가격은 시간에 따라 바뀌므로 주문 가격 스냅샷 생성 시 Product API에서 현재가를 직접 조회한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheClient {

    private final ProductClient productClient;

    /**
     * 상품 정보 조회
     */
    public Map<Long, ProductDTO> getProductsByIds(List<Long> productIds) {
        log.debug("[PRODUCT] 현재가 조회 - productIds: {}", productIds);
        return productClient.getProductsByIds(productIds);
    }

    /**
     * 단일 상품 조회
     */
    public ProductDTO getProductById(Long productId) {
        log.debug("[PRODUCT] 현재가 조회 - productId: {}", productId);
        return productClient.getProductById(productId);
    }
}

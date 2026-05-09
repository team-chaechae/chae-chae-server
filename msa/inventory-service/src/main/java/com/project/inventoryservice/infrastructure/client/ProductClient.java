package com.project.inventoryservice.infrastructure.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Product Service와 통신하기 위한 Feign Client
 */
@FeignClient(name = "product-service")
public interface ProductClient {

    /**
     * 상품 ID 목록의 유효성 검증
     * @param productIds 검증할 상품 ID 목록
     * @return 유효한 상품 ID 목록
     */
    @PostMapping("/api/products/validate")
    List<Long> validateProductIds(@RequestBody List<Long> productIds);
}

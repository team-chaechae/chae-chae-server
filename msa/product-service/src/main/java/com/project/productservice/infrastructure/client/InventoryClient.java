package com.project.productservice.infrastructure.client;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Inventory Service와 통신하기 위한 Feign Client
 */
@FeignClient(name = "inventory-service")
public interface InventoryClient {

    /**
     * 단일 상품의 현재 재고 조회
     * @param productId 상품 ID
     * @return 현재 재고 수량
     */
    @GetMapping("/api/inventory/stock/{productId}")
    Integer getCurrentStock(@PathVariable("productId") Long productId);

    /**
     * 여러 상품의 현재 재고 조회
     * @param productIds 상품 ID 목록
     * @return 상품별 재고 맵
     */
    @PostMapping("/api/inventory/stock/batch")
    Map<Long, Integer> getCurrentStockMap(@RequestBody List<Long> productIds);
}

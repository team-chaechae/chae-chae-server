package com.project.inventoryservice.presentation.controller;

import com.project.inventoryservice.application.service.StockCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 재고 조회 API (2레벨 캐싱 적용)
 *
 * 타 서비스(Product Service 등)에서 호출하는 내부 API
 * L1 (Caffeine) + L2 (Redis) 캐싱으로 고성능 제공
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/stock")
@Tag(name = "Stock Cache API", description = "재고 조회 API (2레벨 캐싱)")
public class StockController {

    private final StockCacheService stockCacheService;

    @Operation(summary = "단일 상품 재고 조회", description = "2레벨 캐싱 적용 (L1: Caffeine 30초, L2: Redis 5분)")
    @GetMapping("/{productId}")
    public ResponseEntity<Integer> getCurrentStock(@PathVariable Long productId) {
        log.debug("Stock query request: productId={}", productId);
        Integer stock = stockCacheService.getCurrentStock(productId);
        return ResponseEntity.ok(stock);
    }

    @Operation(summary = "다중 상품 재고 조회", description = "배치 조회, 2레벨 캐싱 적용")
    @PostMapping("/batch")
    public ResponseEntity<Map<Long, Integer>> getCurrentStockMap(@RequestBody List<Long> productIds) {
        log.debug("Batch stock query request: {} products", productIds.size());
        Map<Long, Integer> stockMap = stockCacheService.getCurrentStockMap(productIds);
        return ResponseEntity.ok(stockMap);
    }

    @Operation(summary = "재고 캐시 무효화", description = "특정 상품의 캐시 삭제 (관리용)")
    @DeleteMapping("/cache/{productId}")
    public ResponseEntity<Void> evictStockCache(@PathVariable Long productId) {
        log.info("Cache eviction request: productId={}", productId);
        stockCacheService.evictStockCache(productId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "전체 재고 캐시 초기화", description = "모든 재고 캐시 삭제 (관리용)")
    @DeleteMapping("/cache")
    public ResponseEntity<Void> evictAllStockCache() {
        log.info("All cache eviction request");
        stockCacheService.evictAllStockCache();
        return ResponseEntity.noContent().build();
    }
}

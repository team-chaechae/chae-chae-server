package com.project.orderservice.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.orderservice.infrastructure.client.dto.ProductDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RBucket;
import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 상품 캐시 클라이언트 (Redisson 기반)
 * Redis Batch 조회 -> Feign fallback
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheClient {

    private static final String CACHE_KEY_PREFIX = "productCache::internal:";

    private final RedissonClient redissonClient;
    private final ProductClient productClient;
    private final ObjectMapper objectMapper;

    /**
     * 상품 정보 조회 (Redis Batch -> Feign fallback)
     */
    public Map<Long, ProductDTO> getProductsByIds(List<Long> productIds) {
        Map<Long, ProductDTO> result = new HashMap<>();
        List<Long> missedIds = new ArrayList<>();

        // 1. Redis 배치 조회
        RBatch batch = redissonClient.createBatch(BatchOptions.defaults());
        Map<Long, RFuture<Object>> futures = new HashMap<>();

        for (Long productId : productIds) {
            String key = CACHE_KEY_PREFIX + productId;
            RFuture<Object> future = batch.getBucket(key).getAsync();
            futures.put(productId, future);
        }

        batch.execute();

        // 결과 처리
        for (Map.Entry<Long, RFuture<Object>> entry : futures.entrySet()) {
            try {
                Object cached = entry.getValue().getNow();
                if (cached != null) {
                    ProductDTO dto = objectMapper.convertValue(cached, ProductDTO.class);
                    result.put(entry.getKey(), dto);
                } else {
                    missedIds.add(entry.getKey());
                }
            } catch (Exception e) {
                log.warn("[CACHE] Redis ERROR - productId: {}, error: {}", entry.getKey(), e.getMessage());
                missedIds.add(entry.getKey());
            }
        }

        // 2. 미스된 것들만 Feign 호출
        if (!missedIds.isEmpty()) {
            try {
                log.info("[CACHE] Feign fallback - productIds: {}", missedIds);
                Map<Long, ProductDTO> fromFeign = productClient.getProductsByIds(missedIds);
                result.putAll(fromFeign);
            } catch (Exception e) {
                log.error("[CACHE] Feign ERROR - productIds: {}, error: {}", missedIds, e.getMessage());
                throw e;
            }
        }

        log.debug("[CACHE] 조회 완료 - total: {}, redisHit: {}, feignCall: {}",
                productIds.size(), productIds.size() - missedIds.size(), missedIds.size());

        return result;
    }

    /**
     * 단일 상품 조회 (Redis -> Feign fallback)
     */
    public ProductDTO getProductById(Long productId) {
        try {
            String key = CACHE_KEY_PREFIX + productId;
            RBucket<Object> bucket = redissonClient.getBucket(key);
            Object cached = bucket.get();

            if (cached != null) {
                return objectMapper.convertValue(cached, ProductDTO.class);
            }
        } catch (Exception e) {
            log.warn("[CACHE] Redis ERROR - productId: {}, error: {}", productId, e.getMessage());
        }

        return productClient.getProductById(productId);
    }
}

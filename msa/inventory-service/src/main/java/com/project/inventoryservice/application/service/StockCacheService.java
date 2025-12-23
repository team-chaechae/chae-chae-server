package com.project.inventoryservice.application.service;

import com.project.inventoryservice.domain.model.StockEntity;
import com.project.inventoryservice.domain.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.redisson.client.codec.IntegerCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 재고 캐시 서비스 (Redisson 기반)
 * 커넥션 풀을 활용한 고성능 Redis 연산
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockCacheService {

    private static final String STOCK_CACHE_PREFIX = "stock:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final long CACHE_TTL_SECONDS = CACHE_TTL.getSeconds();

    private final RedissonClient redissonClient;
    private final StockRepository stockRepository;

    // Lua 스크립트 (재고 차감 - 재고 부족 체크 포함)
    private static final String DECREASE_STOCK_SCRIPT =
            "local current = tonumber(redis.call('GET', KEYS[1]) or 0) " +
            "if current < tonumber(ARGV[1]) then return -1 end " +
            "local newStock = redis.call('DECRBY', KEYS[1], ARGV[1]) " +
            "redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
            "return newStock";

    // Lua 스크립트 (재고 증가)
    private static final String INCREASE_STOCK_SCRIPT =
            "local newStock = redis.call('INCRBY', KEYS[1], ARGV[1]) " +
            "redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
            "return newStock";

    /**
     * 앱 시작 시 DB의 재고 데이터를 Redis에 로드
     */
    @PostConstruct
    public void initStockCache() {
        log.info("[캐시 초기화] DB → Redis 재고 로드 시작");

        List<StockEntity> allStocks = stockRepository.findAll();
        RBatch batch = redissonClient.createBatch(BatchOptions.defaults());

        for (StockEntity stock : allStocks) {
            String cacheKey = STOCK_CACHE_PREFIX + stock.getProductId();
            batch.getBucket(cacheKey, IntegerCodec.INSTANCE)
                    .setAsync(stock.getQuantity(), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        }

        batch.execute();
        log.info("[캐시 초기화] DB → Redis 재고 로드 완료 - {} 건", allStocks.size());
    }

    /**
     * 단일 상품 재고 조회
     * - Redis가 Source of Truth (앱 시작 시 DB에서 로드됨)
     * - 캐시 미스 = 재고 0으로 처리
     */
    public Integer getCurrentStock(Long productId) {
        String cacheKey = STOCK_CACHE_PREFIX + productId;
        RBucket<Integer> bucket = redissonClient.getBucket(cacheKey, IntegerCodec.INSTANCE);

        Integer stock = bucket.get();
        if (stock != null) {
            log.debug("[재고 조회] 상품ID: {}, 재고: {}", productId, stock);
            return stock;
        }

        log.debug("[재고 없음] 상품ID: {} - Redis에 없음, 0 반환", productId);
        return 0;
    }

    /**
     * 다중 상품 재고 조회 (Redisson Batch - 파이프라이닝)
     *
     * 배치 조회 이유:
     * - 단건 조회 N번: N * RTT (네트워크 왕복 시간)
     * - 배치 조회 1번: 1 * RTT (파이프라이닝으로 한번에 처리)
     * - 예: 10개 상품, RTT 1ms → 단건 10ms vs 배치 1ms
     *
     * Redis가 Source of Truth이므로 DB fallback 없음
     */
    public Map<Long, Integer> getCurrentStockMap(List<Long> productIds) {
        Map<Long, Integer> result = new HashMap<>();

        // Redis 배치 조회 (파이프라이닝 - 1번의 네트워크 요청으로 N개 조회)
        RBatch batch = redissonClient.createBatch(BatchOptions.defaults());
        Map<Long, RFuture<Integer>> futures = new HashMap<>();

        for (Long productId : productIds) {
            String cacheKey = STOCK_CACHE_PREFIX + productId;
            RFuture<Integer> future = batch.<Integer>getBucket(cacheKey, IntegerCodec.INSTANCE).getAsync();
            futures.put(productId, future);
        }

        batch.execute();

        // 결과 처리 (없으면 0)
        for (Map.Entry<Long, RFuture<Integer>> entry : futures.entrySet()) {
            try {
                Integer stock = entry.getValue().getNow();
                result.put(entry.getKey(), stock != null ? stock : 0);
            } catch (Exception e) {
                result.put(entry.getKey(), 0);
            }
        }

        log.debug("[배치 재고 조회] 상품 {} 건 조회 완료", productIds.size());
        return result;
    }

    /**
     * 재고 증가 (Lua Script - Atomic 연산)
     */
    public Integer increaseStock(Long productId, Integer amount) {
        String cacheKey = STOCK_CACHE_PREFIX + productId;

        RScript script = redissonClient.getScript(IntegerCodec.INSTANCE);
        Long newStock = script.eval(
                RScript.Mode.READ_WRITE,
                INCREASE_STOCK_SCRIPT,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(cacheKey),
                amount, CACHE_TTL_SECONDS
        );

        if (newStock == null) {
            log.error("[Lua INCRBY 실패] 상품ID: {}", productId);
            throw new RuntimeException("Redis 재고 증가 실패 - 상품ID: " + productId);
        }

        log.debug("[재고 증가] 상품ID: {}, 증가량: {}, 현재: {}", productId, amount, newStock);
        return newStock.intValue();
    }

    /**
     * 재고 감소 (Lua Script - Atomic 연산)
     * 재고 체크 + DECRBY + EXPIRE를 1번의 Redis 호출로 처리
     */
    public Integer decreaseStock(Long productId, Integer amount) {
        String cacheKey = STOCK_CACHE_PREFIX + productId;

        RScript script = redissonClient.getScript(IntegerCodec.INSTANCE);
        Long newStock = script.eval(
                RScript.Mode.READ_WRITE,
                DECREASE_STOCK_SCRIPT,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(cacheKey),
                amount, CACHE_TTL_SECONDS
        );

        if (newStock == null) {
            throw new RuntimeException("Redis 재고 차감 실패 - 상품ID: " + productId);
        }

        // -1은 재고 부족을 의미
        if (newStock == -1) {
            log.warn("[재고 부족] 상품ID: {}, 시도 감소량: {}", productId, amount);
            throw new RuntimeException("재고 부족 - 상품ID: " + productId);
        }

        log.debug("[재고 차감] 상품ID: {}, 차감량: {}, 현재: {}", productId, amount, newStock);
        return newStock.intValue();
    }

    /**
     * 재고 캐시 직접 업데이트
     */
    public void updateStockCache(Long productId, Integer newStock) {
        String cacheKey = STOCK_CACHE_PREFIX + productId;
        RBucket<Integer> bucket = redissonClient.getBucket(cacheKey, IntegerCodec.INSTANCE);
        bucket.set(newStock, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        log.info("[캐시 갱신] Redis 재고 - 상품ID: {}, 재고: {}", productId, newStock);
    }

    /**
     * 재고 캐시 무효화
     */
    public void evictStockCache(Long productId) {
        String cacheKey = STOCK_CACHE_PREFIX + productId;
        redissonClient.getBucket(cacheKey).delete();
        log.info("[캐시 무효화] Redis 재고 - 상품ID: {}", productId);
    }

    /**
     * 다중 상품 재고 캐시 무효화
     */
    public void evictStockCacheBatch(List<Long> productIds) {
        RBatch batch = redissonClient.createBatch(BatchOptions.defaults());
        for (Long productId : productIds) {
            String cacheKey = STOCK_CACHE_PREFIX + productId;
            batch.getBucket(cacheKey).deleteAsync();
        }
        batch.execute();
        log.info("[캐시 무효화] Redis 재고 - {} 건", productIds.size());
    }

    /**
     * 전체 재고 캐시 초기화
     */
    public void evictAllStockCache() {
        RKeys keys = redissonClient.getKeys();
        keys.deleteByPattern(STOCK_CACHE_PREFIX + "*");
        log.info("[캐시 무효화] Redis 재고 전체 초기화");
    }
}

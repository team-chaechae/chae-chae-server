package com.project.productservice.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * 2레벨 캐시 서비스 (Redisson 기반)
 * L1: Redis (분산) - order-service에서 직접 조회
 * L2: Caffeine (로컬) - product-service 내부 조회용
 */
@Slf4j
@Service
public class TwoLevelCacheService {

    private final CacheManager caffeineCacheManager;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public TwoLevelCacheService(CacheManager caffeineCacheManager, RedissonClient redissonClient) {
        this.caffeineCacheManager = caffeineCacheManager;
        this.redissonClient = redissonClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 캐시에서 값 조회 (Caffeine -> Redis -> DB)
     */
    public <T> T get(String cacheName, Object key, Class<T> type, Supplier<T> dbSupplier) {
        Cache caffeineCache = caffeineCacheManager.getCache(cacheName);

        // 1. Caffeine 캐시 조회
        if (caffeineCache != null) {
            Cache.ValueWrapper cachedValue = caffeineCache.get(key);
            if (cachedValue != null) {
                log.info("[CACHE] Caffeine HIT - key: {}", key);
                return convertValue(cachedValue.get(), type);
            }
        }

        // 2. Redis 조회
        String redisKey = cacheName + "::" + key;
        try {
            RBucket<Object> bucket = redissonClient.getBucket(redisKey);
            Object redisValue = bucket.get();
            if (redisValue != null) {
                log.info("[CACHE] Redis HIT - key: {}", redisKey);
                T result = convertValue(redisValue, type);
                // Caffeine에도 저장
                if (caffeineCache != null) {
                    caffeineCache.put(key, result);
                }
                return result;
            }
        } catch (Exception e) {
            log.warn("[CACHE] Redis ERROR - key: {}, error: {}", redisKey, e.getMessage());
        }

        // 3. DB 조회
        T dbValue = dbSupplier.get();
        log.info("[CACHE] DB Query - key: {}", key);

        // 캐시에 저장
        if (dbValue != null) {
            put(cacheName, key, dbValue);
        }

        return dbValue;
    }

    /**
     * 캐시에 직접 값 저장 (L1 + L2)
     */
    public void put(String cacheName, Object key, Object value) {
        String redisKey = cacheName + "::" + key;

        // L1 (Redisson - 분산)
        try {
            RBucket<Object> bucket = redissonClient.getBucket(redisKey);
            bucket.set(value);
            log.debug("[CACHE PUT] L1 Redis - key: {}", redisKey);
        } catch (Exception e) {
            log.error("[CACHE PUT] L1 Redis 실패 - key: {}, error: {}", redisKey, e.getMessage());
        }

        // L2 (Caffeine - 로컬)
        Cache caffeineCache = caffeineCacheManager.getCache(cacheName);
        if (caffeineCache != null) {
            caffeineCache.put(key, value);
            log.debug("[CACHE PUT] L2 Caffeine - cache: {}, key: {}", cacheName, key);
        }
    }

    /**
     * 캐시 무효화 (L1 + L2)
     */
    public void evict(String cacheName, Object key) {
        // L1 (Redis)
        String redisKey = cacheName + "::" + key;
        redissonClient.getBucket(redisKey).delete();
        log.debug("[CACHE] L1 EVICT - key: {}", redisKey);

        // L2 (Caffeine)
        Cache l2Cache = caffeineCacheManager.getCache(cacheName);
        if (l2Cache != null) {
            l2Cache.evict(key);
            log.debug("[CACHE] L2 EVICT - cache: {}, key: {}", cacheName, key);
        }
    }

    /**
     * 캐시 전체 무효화 (L1 + L2)
     */
    public void evictAll(String cacheName) {
        // L1 (Redis)
        RKeys keys = redissonClient.getKeys();
        keys.deleteByPattern(cacheName + "::*");
        log.debug("L1 Cache CLEAR - pattern: {}::*", cacheName);

        // L2 (Caffeine)
        Cache l2Cache = caffeineCacheManager.getCache(cacheName);
        if (l2Cache != null) {
            l2Cache.clear();
            log.debug("L2 Cache CLEAR - cacheName: {}", cacheName);
        }
    }

    /**
     * LinkedHashMap 등을 올바른 타입으로 변환
     */
    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        return objectMapper.convertValue(value, type);
    }
}

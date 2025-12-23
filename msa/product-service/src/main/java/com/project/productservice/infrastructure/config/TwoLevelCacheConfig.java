package com.project.productservice.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.List;

/**
 * 2레벨 캐시 설정
 * L1: Redis (분산 캐시) - order-service에서 직접 조회
 * L2: Caffeine (로컬 메모리 캐시) - product-service 내부 조회용
 */
@Slf4j
@Configuration
public class TwoLevelCacheConfig {

    // 캐시 이름 상수
    public static final String PRODUCT_CACHE = "productCache";
    public static final String PRODUCT_LIST_CACHE = "productListCache";

    // L1 캐시 설정
    private static final int L1_MAX_SIZE = 500;

    @Value("${spring.data.redis.sentinel.master}")
    private String sentinelMaster;

    @Value("${REDIS_SENTINEL_1:localhost}:${REDIS_SENTINEL_PORT_1:26379}")
    private String sentinelNode1;

    @Value("${REDIS_SENTINEL_2:localhost}:${REDIS_SENTINEL_PORT_2:26380}")
    private String sentinelNode2;

    @Value("${REDIS_SENTINEL_3:localhost}:${REDIS_SENTINEL_PORT_3:26381}")
    private String sentinelNode3;

    /**
     * L2 캐시 매니저 (Caffeine - 로컬)
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(getCacheNames());
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(L1_MAX_SIZE)
                .recordStats()
        );

        log.info("L1 Cache (Caffeine) initialized - maxSize: {}", L1_MAX_SIZE);
        return cacheManager;
    }

    /**
     * L1 캐시 (Redisson - 분산)
     * Sentinel 모드로 고가용성 보장
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.deactivateDefaultTyping();

        Config config = new Config();
        config.setCodec(new org.redisson.codec.TypedJsonJacksonCodec(Object.class, objectMapper));

        // Sentinel 모드 설정 (로컬 개발환경에서는 Master만 사용)
        config.useSentinelServers()
                .setMasterName(sentinelMaster)
                .addSentinelAddress(
                        "redis://" + sentinelNode1,
                        "redis://" + sentinelNode2,
                        "redis://" + sentinelNode3)
                .setReadMode(ReadMode.MASTER)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        log.info("L1 Cache (Redisson) initialized - sentinel master: {}, nodes: [{}, {}, {}]",
                sentinelMaster, sentinelNode1, sentinelNode2, sentinelNode3);
        return Redisson.create(config);
    }

    private List<String> getCacheNames() {
        return Arrays.asList(PRODUCT_CACHE, PRODUCT_LIST_CACHE);
    }
}

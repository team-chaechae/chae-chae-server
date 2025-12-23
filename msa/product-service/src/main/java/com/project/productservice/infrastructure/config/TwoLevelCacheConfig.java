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

    @Value("${REDIS_HOST:localhost}")
    private String redisHost;

    @Value("${REDIS_PORT:6379}")
    private int redisPort;

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
     * TypeReferencedCodec으로 클래스명 없이 순수 JSON 저장
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.deactivateDefaultTyping();

        Config config = new Config();
        config.setCodec(new org.redisson.codec.TypedJsonJacksonCodec(Object.class, objectMapper));
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort);

        log.info("L1 Cache (Redisson) initialized - address: redis://{}:{}", redisHost, redisPort);
        return Redisson.create(config);
    }

    private List<String> getCacheNames() {
        return Arrays.asList(PRODUCT_CACHE, PRODUCT_LIST_CACHE);
    }
}

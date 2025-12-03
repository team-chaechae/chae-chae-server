package com.project.userservice.infrastructure.service;

import com.project.userservice.application.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Override
    public void addToBlacklist(String token, long expiration) {
        if (expiration <= 0) {
            log.debug("토큰이 이미 만료되어 블랙리스트에 추가하지 않음");
            return;
        }

        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "logout", expiration, TimeUnit.SECONDS);
        log.info("토큰 블랙리스트 추가 완료 (TTL: {}초)", expiration);
    }

    @Override
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}

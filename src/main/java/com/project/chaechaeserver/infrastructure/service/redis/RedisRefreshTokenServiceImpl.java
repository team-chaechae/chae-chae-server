package com.project.chaechaeserver.infrastructure.service.redis;

import com.project.chaechaeserver.application.service.user.RedisRefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisRefreshTokenServiceImpl implements RedisRefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    public void saveRefreshToken(String email, String refreshToken) {
        long refreshTokenTime = 14 * 24 * 60 * 60L;
        redisTemplate.opsForValue().set(email, refreshToken, Duration.ofSeconds(refreshTokenTime));
    }

    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get(email);
    }

    public void deleteRefreshToken(String email) {
        redisTemplate.delete(email);
    }

}

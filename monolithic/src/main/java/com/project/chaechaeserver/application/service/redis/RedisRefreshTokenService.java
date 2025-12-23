package com.project.chaechaeserver.application.service.redis;


public interface RedisRefreshTokenService {

    void saveRefreshToken(String email, String refreshToken);

    String getRefreshToken(String email);

    void deleteRefreshToken(String email);
}
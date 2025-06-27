package com.project.chaechaeserver.application.service.user;


public interface RedisRefreshTokenService {

    void saveRefreshToken(String email, String refreshToken);

    String getRefreshToken(String email);

    void deleteRefreshToken(String email);
}
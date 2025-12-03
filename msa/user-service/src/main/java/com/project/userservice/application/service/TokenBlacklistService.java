package com.project.userservice.application.service;

public interface TokenBlacklistService {

    /**
     * 토큰을 블랙리스트에 추가
     * @param token JWT 토큰
     * @param expiration 토큰 만료까지 남은 시간 (초)
     */
    void addToBlacklist(String token, long expiration);

    /**
     * 토큰이 블랙리스트에 있는지 확인
     * @param token JWT 토큰
     * @return 블랙리스트에 있으면 true
     */
    boolean isBlacklisted(String token);
}

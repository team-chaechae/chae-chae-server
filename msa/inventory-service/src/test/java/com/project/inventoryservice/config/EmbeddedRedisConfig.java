package com.project.inventoryservice.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import java.io.IOException;

@Profile("test")
@Configuration
public class EmbeddedRedisConfig {

    @Value("${spring.data.redis.port:6380}")
    private int redisPort;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        redisServer = new RedisServer(redisPort);
        try {
            redisServer.start();
        } catch (Exception e) {
            // 이미 실행중인 경우 무시
        }
    }

    @PreDestroy
    public void stopRedis() throws IOException {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }
}

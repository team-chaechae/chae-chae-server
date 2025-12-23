package com.project.orderservice.infrastructure.kafka.backpressure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class BackpressureConfig {

    @Value("${kafka.backpressure.max-permits:50}")
    private int maxPermits;

    @Value("${kafka.backpressure.core-pool-size:5}")
    private int corePoolSize;

    @Value("${kafka.backpressure.max-pool-size:20}")
    private int maxPoolSize;

    @Value("${kafka.backpressure.acquire-timeout-ms:30000}")
    private long acquireTimeoutMs;

    @Bean
    public BlockingThreadPoolExecutor kafkaBackpressureExecutor() {
        return new BlockingThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                maxPermits,
                acquireTimeoutMs,
                new LinkedBlockingQueue<>(1000),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("kafka-backpressure-" + counter.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    }
                }
        );
    }
}

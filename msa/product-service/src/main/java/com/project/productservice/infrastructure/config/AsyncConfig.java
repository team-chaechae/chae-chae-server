package com.project.productservice.infrastructure.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 설정
 * 29CM 사례에서 언급된 Graceful Shutdown 설정 포함
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Outbox 이벤트 발행용 Async Executor
     * Graceful Shutdown을 위한 설정 포함:
     * - setWaitForTasksToCompleteOnShutdown(true): shutdown 시 현재 실행중인 작업 완료 대기
     * - setAwaitTerminationSeconds(10): 최대 10초간 대기
     */
    @Bean(name = "outboxAsyncExecutor")
    public Executor outboxAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("outbox-async-");

        // Graceful Shutdown 설정 (29CM 사례 참고)
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);

        executor.initialize();
        return executor;
    }
}

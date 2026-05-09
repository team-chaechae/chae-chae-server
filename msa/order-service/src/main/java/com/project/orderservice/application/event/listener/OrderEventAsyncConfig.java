package com.project.orderservice.application.event.listener;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class OrderEventAsyncConfig {

    public static final String ORDER_EVENT_ASYNC_TASK_EXECUTOR = "orderEventAsyncTaskExecutor";

    @Bean(name = ORDER_EVENT_ASYNC_TASK_EXECUTOR)
    public ThreadPoolTaskExecutor orderEventAsyncTaskExecutor(
            @Value("${order.event.async.core-pool-size:4}") int corePoolSize,
            @Value("${order.event.async.max-pool-size:16}") int maxPoolSize,
            @Value("${order.event.async.queue-capacity:1000}") int queueCapacity,
            @Value("${order.event.async.await-termination-seconds:30}") int awaitTerminationSeconds
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("order-event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        return executor;
    }
}

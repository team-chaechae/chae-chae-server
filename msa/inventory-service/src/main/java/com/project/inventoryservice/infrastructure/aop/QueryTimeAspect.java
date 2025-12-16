package com.project.inventoryservice.infrastructure.aop;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Repository 레이어의 쿼리 실행 시간을 측정하는 AOP Aspect
 * - Micrometer Timer로 메트릭 기록
 * - Prometheus에서 scrape → Grafana에서 시각화
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class QueryTimeAspect {

    private final MeterRegistry meterRegistry;

    private static final String METRIC_NAME = "repository.query.time";
    private static final long SLOW_QUERY_THRESHOLD_MS = 100;

    /**
     * Repository 인터페이스 및 구현체의 모든 메서드 실행 시간 측정
     */
    @Around("execution(* com.project.inventoryservice.infrastructure.repository..*(..))")
    public Object measureQueryTime(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        long startTime = System.nanoTime();

        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.nanoTime() - startTime;
            long durationMs = TimeUnit.NANOSECONDS.toMillis(duration);

            // Micrometer Timer 기록
            Timer.builder(METRIC_NAME)
                    .tag("class", className)
                    .tag("method", methodName)
                    .description("Repository query execution time")
                    .register(meterRegistry)
                    .record(duration, TimeUnit.NANOSECONDS);

            // 슬로우 쿼리 로깅 (100ms 이상)
            if (durationMs >= SLOW_QUERY_THRESHOLD_MS) {
                log.warn("[SLOW QUERY] {}.{} took {}ms", className, methodName, durationMs);
            } else if (log.isDebugEnabled()) {
                log.debug("[QUERY] {}.{} took {}ms", className, methodName, durationMs);
            }
        }
    }
}

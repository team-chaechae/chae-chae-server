package com.project.chaechaeserver.application.global.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 트랜잭션 실행 시간 측정 AOP
 *
 * <p>@Transactional이 선언된 모든 메서드의 실행 시간을 측정하여 로그로 출력합니다.
 * 트랜잭션이 길어지는 구간을 파악하는데 유용합니다.
 */
@Slf4j
@Aspect
@Component
public class TransactionTimeMonitor {

    @Around("@annotation(transactional)")
    public Object measureTransactionTime(ProceedingJoinPoint joinPoint, Transactional transactional) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();
        log.info("[TX 시작] {}.{} - 전파: {}",
            className, methodName, transactional.propagation());

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            log.info("[TX 종료] {}.{} - 소요시간: {}ms",
                className, methodName, executionTime);

            // 경고: 트랜잭션이 1초 이상 걸리는 경우
            if (executionTime > 1000) {
                log.warn("[TX 경고] {}.{} - 트랜잭션이 {}ms 소요됨 (1초 초과)",
                    className, methodName, executionTime);
            }

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[TX 실패] {}.{} - 소요시간: {}ms, 예외: {}",
                className, methodName, executionTime, e.getClass().getSimpleName());
            throw e;
        }
    }
}

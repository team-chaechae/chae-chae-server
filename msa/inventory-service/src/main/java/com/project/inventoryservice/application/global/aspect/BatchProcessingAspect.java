package com.project.inventoryservice.application.global.aspect;

import com.project.inventoryservice.application.global.annotation.BatchProcessing;
import com.project.inventoryservice.presentation.request.bulk.ReqBulkCreateInventoryDTO;
import com.project.inventoryservice.presentation.request.bulk.ReqUpdateInventoryDTO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 배치 처리 로깅 AOP
 * @BatchProcessing 어노테이션이 붙은 메서드의 실행을 모니터링합니다.
 */
@Slf4j
@Aspect
@Component
public class BatchProcessingAspect {

    @Around("@annotation(com.project.inventoryservice.application.global.annotation.BatchProcessing)")
    public Object logBatchProcessing(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        BatchProcessing annotation = signature.getMethod().getAnnotation(BatchProcessing.class);

        String batchName = annotation.value();
        int batchSize = annotation.batchSize();

        // 메서드 파라미터에서 총 아이템 개수 추출
        Object[] args = joinPoint.getArgs();
        int totalItems = extractTotalItems(args);

        // 배치 시작 로깅
        log.info("========================================");
        log.info("배치 처리 시작: {}", batchName);
        log.info("총 처리 대상: {}개 | 배치 사이즈: {}개", totalItems, batchSize);
        log.info("예상 배치 횟수: {}회", calculateBatchCount(totalItems, batchSize));
        log.info("========================================");

        long startTime = System.currentTimeMillis();

        try {
            // 실제 배치 처리 실행
            Object result = joinPoint.proceed();

            // 배치 완료 로깅
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.info("========================================");
            log.info("배치 처리 완료: {}", batchName);
            log.info("처리 시간: {}ms ({}초)", duration, duration / 1000.0);
            log.info("처리량: {}개/초", calculateThroughput(totalItems, duration));
            log.info("========================================");

            return result;

        } catch (Exception e) {
            // 배치 실패 로깅
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.error("========================================");
            log.error("배치 처리 실패: {}", batchName);
            log.error("처리 시간: {}ms", duration);
            log.error("에러 메시지: {}", e.getMessage());
            log.error("========================================", e);

            throw e;
        }
    }

    /**
     * 메서드 파라미터에서 총 아이템 개수 추출
     */
    private int extractTotalItems(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof List<?> list) {
                return list.size();
            }
            if (arg instanceof ReqBulkCreateInventoryDTO dto) {
                return dto.getInventory() != null ? dto.getInventory().size() : 0;
            }
            if (arg instanceof ReqUpdateInventoryDTO dto) {
                return dto.getInventory() != null ? dto.getInventory().size() : 0;
            }
        }
        return 0;
    }

    /**
     * 배치 횟수 계산
     */
    private int calculateBatchCount(int totalItems, int batchSize) {
        if (totalItems == 0 || batchSize == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalItems / batchSize);
    }

    /**
     * 처리량 계산 (개/초)
     */
    private double calculateThroughput(int totalItems, long durationMs) {
        if (durationMs == 0) {
            return 0;
        }
        return (double) totalItems / (durationMs / 1000.0);
    }
}

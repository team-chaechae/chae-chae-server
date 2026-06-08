package com.project.inventoryservice.application.service.sales;

import com.project.inventoryservice.application.response.sales.ResSalesInventoryDTO;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesInventoryIdempotencyService {

    private static final String COMPLETED_KEY_PREFIX = "inventory:sales:completed:";
    private static final String PROCESSING_KEY_PREFIX = "inventory:sales:processing:";
    private static final Duration COMPLETED_TTL = Duration.ofDays(7);
    private static final Duration PROCESSING_TTL = Duration.ofMinutes(10);

    private final RedissonClient redissonClient;

    public ResSalesInventoryDTO execute(
            String operationId,
            Supplier<ResSalesInventoryDTO> command
    ) {
        if (operationId == null || operationId.isBlank()) {
            return command.get();
        }

        RBucket<String> completedBucket = redissonClient.getBucket(COMPLETED_KEY_PREFIX + operationId);
        if (completedBucket.isExists()) {
            log.info("[Sales Inventory 멱등성 중복 완료 스킵] operationId: {}", operationId);
            return ResSalesInventoryDTO.duplicate();
        }

        RBucket<String> processingBucket = redissonClient.getBucket(PROCESSING_KEY_PREFIX + operationId);
        if (!processingBucket.setIfAbsent("PROCESSING", PROCESSING_TTL)) {
            if (completedBucket.isExists()) {
                log.info("[Sales Inventory 멱등성 중복 완료 스킵] operationId: {}", operationId);
                return ResSalesInventoryDTO.duplicate();
            }
            throw new IllegalStateException("이미 처리 중인 재고 명령입니다. operationId: " + operationId);
        }

        try {
            ResSalesInventoryDTO result = command.get();
            completedBucket.set("COMPLETED", COMPLETED_TTL);
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("재고 명령 처리 실패. operationId: " + operationId, e);
        } finally {
            processingBucket.delete();
        }
    }
}

package com.project.inventoryservice.application.service;

import com.project.inventoryservice.infrastructure.kafka.InventoryEvent;
import com.project.inventoryservice.infrastructure.kafka.InventoryEventProducer;
import com.project.inventoryservice.infrastructure.kafka.InventoryConfirmedEventProducer;
import com.project.inventoryservice.infrastructure.kafka.InventoryFailedEventProducer;
import com.project.inventoryservice.infrastructure.kafka.dto.InventoryConfirmedEvent;
import com.project.inventoryservice.infrastructure.kafka.dto.InventoryFailedEvent;
import com.project.inventoryservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 재고 차감 서비스
 * 결제 완료 후 재고 차감 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryDeductionService {

    private static final String DEDUCTION_IDEMPOTENCY_PREFIX = "inventory:deduction:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final StockCacheService stockCacheService;
    private final InventoryEventProducer inventoryEventProducer;
    private final InventoryConfirmedEventProducer inventoryConfirmedEventProducer;
    private final InventoryFailedEventProducer inventoryFailedEventProducer;
    private final RedissonClient redissonClient;

    /**
     * 결제 완료 이벤트 처리 - 재고 차감
     */
    public void processPaymentCompleted(PaymentCompletedEvent event) {
        String orderId = event.getOrderId();
        Long salesId = event.getSalesId();
        List<PaymentCompletedEvent.OrderItem> items = event.getItems();

        log.info("[결제 완료 이벤트 수신] orderId: {}, salesId: {}, 상품 수: {}",
                orderId, salesId, items != null ? items.size() : 0);

        // 중복 재고 차감 방지 - 원자적 멱등성 체크 (SETNX)
        if (!tryAcquireIdempotencyLock(orderId)) {
            log.info("[중복 재고 차감 스킵] 이미 처리된 주문 - orderId: {}, salesId: {}", orderId, salesId);
            return;
        }

        if (items == null || items.isEmpty()) {
            log.warn("[재고 차감 스킵] 상품 목록 없음 - orderId: {}", orderId);
            return;
        }

        // 재고 차감 시도
        List<Long> successfullyDeducted = new ArrayList<>();
        List<Integer> deductedQuantities = new ArrayList<>();
        boolean allSuccess = true;
        String failureReason = null;

        for (PaymentCompletedEvent.OrderItem item : items) {
            try {
                // Redis에서 재고 차감
                Integer currentStock = stockCacheService.decreaseStock(item.getProductId(), item.getQuantity());

                successfullyDeducted.add(item.getProductId());
                deductedQuantities.add(item.getQuantity());

                // Kafka 이벤트 발행 (CONFIRMED 상태로 inventory INSERT)
                InventoryEvent inventoryEvent = InventoryEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .productId(item.getProductId())
                        .quantity(-item.getQuantity())
                        .changeType("ORDER_DECREASE")
                        .orderId(orderId)
                        .status("CONFIRMED")
                        .occurredAt(LocalDateTime.now())
                        .currentStock(currentStock)
                        .build();
                inventoryEventProducer.publish(inventoryEvent);

                log.debug("[재고 차감 성공] orderId: {}, productId: {}, 차감량: {}, 현재재고: {}",
                        orderId, item.getProductId(), item.getQuantity(), currentStock);

            } catch (RuntimeException e) {
                allSuccess = false;
                failureReason = "상품 " + item.getProductId() + ": " + e.getMessage();
                log.warn("[재고 차감 실패] orderId: {}, productId: {}, 사유: {}",
                        orderId, item.getProductId(), e.getMessage());
                break;
            }
        }

        if (allSuccess) {
            // 성공 - inventory-confirmed 이벤트 발행 (SSE 알림용)
            InventoryConfirmedEvent confirmedEvent = InventoryConfirmedEvent.of(orderId, salesId);
            inventoryConfirmedEventProducer.publish(confirmedEvent);

            log.info("[재고 차감 완료] orderId: {}, salesId: {}", orderId, salesId);
        } else {
            // 실패 시 멱등성 키 삭제 (재시도 가능하도록)
            releaseIdempotencyLock(orderId);

            // 이미 차감된 것들 롤백
            rollbackDeductedStock(orderId, successfullyDeducted, deductedQuantities);

            // inventory-failed 이벤트 발행 → payment-service에서 환불 처리
            InventoryFailedEvent failedEvent = InventoryFailedEvent.of(orderId, salesId, failureReason);
            inventoryFailedEventProducer.publish(failedEvent);

            log.warn("[재고 차감 실패 - 환불 요청] orderId: {}, salesId: {}, 사유: {}",
                    orderId, salesId, failureReason);
        }
    }

    /**
     * 재고 롤백
     */
    private void rollbackDeductedStock(String orderId, List<Long> productIds, List<Integer> quantities) {
        if (productIds.isEmpty()) {
            return;
        }

        log.warn("[재고 롤백 시작] orderId: {}, 롤백 대상: {}", orderId, productIds);

        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            Integer quantity = quantities.get(i);
            try {
                stockCacheService.increaseStock(productId, quantity);
                log.info("[재고 롤백 완료] orderId: {}, productId: {}, 복구량: {}",
                        orderId, productId, quantity);
            } catch (Exception e) {
                log.error("[재고 롤백 실패] orderId: {}, productId: {}, 에러: {}",
                        orderId, productId, e.getMessage());
            }
        }
    }

    /**
     * 멱등성 락 획득 (원자적 SETNX)
     * - 키가 없으면 설정하고 true 반환 (처리 진행)
     * - 키가 있으면 false 반환 (이미 처리중/완료)
     *
     * 기존 isExists() + set() 대비:
     * - RTT: 2회 → 1회 (성능 향상)
     * - 원자성: 보장 (race condition 방지)
     */
    private boolean tryAcquireIdempotencyLock(String orderId) {
        String key = DEDUCTION_IDEMPOTENCY_PREFIX + orderId;
        RBucket<String> bucket = redissonClient.getBucket(key);
        boolean acquired = bucket.setIfAbsent("PROCESSING", IDEMPOTENCY_TTL);
        if (acquired) {
            log.debug("[멱등성 락 획득] orderId: {}, TTL: {}", orderId, IDEMPOTENCY_TTL);
        }
        return acquired;
    }

    /**
     * 멱등성 락 해제 (실패 시 재시도 가능하도록)
     */
    private void releaseIdempotencyLock(String orderId) {
        String key = DEDUCTION_IDEMPOTENCY_PREFIX + orderId;
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.delete();
        log.debug("[멱등성 락 해제] orderId: {}", orderId);
    }
}

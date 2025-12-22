package com.project.inventoryservice.application.service;

import com.project.inventoryservice.application.response.ResConfirmStockDTO;
import com.project.inventoryservice.application.response.ResReleaseStockDTO;
import com.project.inventoryservice.application.response.ResReserveStockDTO;
import com.project.inventoryservice.infrastructure.kafka.InventoryEvent;
import com.project.inventoryservice.infrastructure.kafka.InventoryEventProducer;
import com.project.inventoryservice.infrastructure.repository.JdbcInventoryRepository;
import com.project.inventoryservice.presentation.request.ReqConfirmStockDTO;
import com.project.inventoryservice.presentation.request.ReqReleaseStockDTO;
import com.project.inventoryservice.presentation.request.ReqReserveStockDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 재고 예약 서비스 (선차감 구조)
 *
 * 흐름:
 * 1. 예약(주문 생성): Redis stock 선차감 + inventory 테이블에 RESERVED 상태로 INSERT
 * 2. 확정(결제 완료): inventory 테이블 status를 CONFIRMED로 UPDATE
 * 3. 취소: Redis stock 복구 + inventory 테이블 status를 CANCELLED로 UPDATE
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final StockCacheService stockCacheService;
    private final InventoryEventProducer eventProducer;
    private final JdbcInventoryRepository jdbcInventoryRepository;

    /**
     * 재고 예약 (선차감)
     * - Redis: stock 직접 차감
     * - Kafka: inventory-events 발행 (RESERVED 상태)
     */
    public ResReserveStockDTO reserveStock(ReqReserveStockDTO request) {
        String orderId = request.getOrderId();
        Long salesId = request.getSalesId();
        List<ReqReserveStockDTO.ReserveItem> items = request.getItems();

        log.info("[재고 선차감 시작] orderId: {}, salesId: {}, 상품 수: {}", orderId, salesId, items.size());

        List<ResReserveStockDTO.ItemResult> results = new ArrayList<>();
        List<Long> successfullyDeducted = new ArrayList<>();
        List<Integer> deductedQuantities = new ArrayList<>();
        boolean allSuccess = true;
        String failureReason = null;

        for (ReqReserveStockDTO.ReserveItem item : items) {
            try {
                // Redis에서 직접 차감
                Integer currentStock = stockCacheService.decreaseStock(item.getProductId(), item.getQuantity());

                successfullyDeducted.add(item.getProductId());
                deductedQuantities.add(item.getQuantity());

                // Kafka 이벤트 발행 (RESERVED 상태로 inventory INSERT 용)
                InventoryEvent event = InventoryEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .productId(item.getProductId())
                        .quantity(-item.getQuantity())
                        .changeType("ORDER_DECREASE")
                        .orderId(orderId)
                        .status("RESERVED")
                        .occurredAt(LocalDateTime.now())
                        .currentStock(currentStock)
                        .build();
                eventProducer.publish(event);

                results.add(ResReserveStockDTO.ItemResult.builder()
                        .productId(item.getProductId())
                        .requestedQuantity(item.getQuantity())
                        .reservedQuantity(item.getQuantity())
                        .availableStock(currentStock)
                        .success(true)
                        .build());

                log.debug("[재고 선차감 성공] orderId: {}, productId: {}, 차감량: {}, 현재재고: {}",
                        orderId, item.getProductId(), item.getQuantity(), currentStock);

            } catch (RuntimeException e) {
                allSuccess = false;
                failureReason = "상품 " + item.getProductId() + ": " + e.getMessage();

                results.add(ResReserveStockDTO.ItemResult.builder()
                        .productId(item.getProductId())
                        .requestedQuantity(item.getQuantity())
                        .reservedQuantity(0)
                        .availableStock(stockCacheService.getCurrentStock(item.getProductId()))
                        .success(false)
                        .errorReason(e.getMessage())
                        .build());

                log.warn("[재고 선차감 실패] orderId: {}, productId: {}, 사유: {}",
                        orderId, item.getProductId(), e.getMessage());
                break;
            }
        }

        // 실패 시 이미 차감된 것들 롤백
        if (!allSuccess && !successfullyDeducted.isEmpty()) {
            log.warn("[선차감 롤백 시작] orderId: {}, 롤백 대상: {}", orderId, successfullyDeducted);
            for (int i = 0; i < successfullyDeducted.size(); i++) {
                Long productId = successfullyDeducted.get(i);
                Integer quantity = deductedQuantities.get(i);
                try {
                    stockCacheService.increaseStock(productId, quantity);
                    log.info("[선차감 롤백 완료] orderId: {}, productId: {}, 복구량: {}",
                            orderId, productId, quantity);
                } catch (Exception e) {
                    log.error("[선차감 롤백 실패] orderId: {}, productId: {}, 에러: {}",
                            orderId, productId, e.getMessage());
                }
            }
        }

        if (allSuccess) {
            log.info("[재고 선차감 완료] orderId: {}, salesId: {}", orderId, salesId);
            return ResReserveStockDTO.success(orderId, salesId, results);
        } else {
            log.warn("[재고 선차감 실패] orderId: {}, salesId: {}, 사유: {}", orderId, salesId, failureReason);
            return ResReserveStockDTO.failed(orderId, salesId, results, failureReason);
        }
    }

    /**
     * 재고 확정 (결제 완료)
     * - inventory 테이블: status를 CONFIRMED로 UPDATE
     * - Redis: 이미 차감되어 있으므로 건드리지 않음
     */
    public ResConfirmStockDTO confirmStock(ReqConfirmStockDTO request) {
        String orderId = request.getOrderId();
        Long salesId = request.getSalesId();

        log.info("[재고 확정 시작] orderId: {}, salesId: {}", orderId, salesId);

        try {
            int updated = jdbcInventoryRepository.confirmByOrderId(orderId);

            if (updated > 0) {
                log.info("[재고 확정 완료] orderId: {}, salesId: {}, 변경 건수: {}", orderId, salesId, updated);
                return ResConfirmStockDTO.success(orderId, salesId);
            } else {
                log.warn("[재고 확정] 변경된 레코드 없음 - orderId: {}", orderId);
                return ResConfirmStockDTO.success(orderId, salesId);
            }
        } catch (Exception e) {
            log.error("[재고 확정 실패] orderId: {}, 에러: {}", orderId, e.getMessage());
            return ResConfirmStockDTO.failed(orderId, salesId, e.getMessage());
        }
    }

    /**
     * 예약 해제 (취소/보상 트랜잭션)
     * - Redis: stock 복구
     * - inventory 테이블: status를 CANCELLED로 UPDATE
     */
    public ResReleaseStockDTO releaseStock(ReqReleaseStockDTO request) {
        String orderId = request.getOrderId();
        Long salesId = request.getSalesId();
        List<ReqReleaseStockDTO.ReleaseItem> items = request.getItems();

        log.info("[재고 복구 시작] orderId: {}, salesId: {}, 상품 수: {}, 사유: {}",
                orderId, salesId, items.size(), request.getReason());

        boolean allSuccess = true;
        StringBuilder failureReasons = new StringBuilder();

        // Redis 재고 복구
        for (ReqReleaseStockDTO.ReleaseItem item : items) {
            try {
                // quantity가 없으면 DB에서 조회해야 하지만, 현재는 items에 quantity가 포함되어 있다고 가정
                // TODO: 필요 시 DB에서 orderId로 quantity 조회
                if (item.getQuantity() != null && item.getQuantity() > 0) {
                    Integer currentStock = stockCacheService.increaseStock(item.getProductId(), item.getQuantity());

                    // 복구 이벤트 발행
                    InventoryEvent event = InventoryEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .changeType("ORDER_RESTORE")
                            .orderId(orderId)
                            .status("CANCELLED")
                            .occurredAt(LocalDateTime.now())
                            .currentStock(currentStock)
                            .build();
                    eventProducer.publish(event);

                    log.info("[재고 복구 성공] orderId: {}, productId: {}, 복구량: {}, 현재재고: {}",
                            orderId, item.getProductId(), item.getQuantity(), currentStock);
                }
            } catch (Exception e) {
                allSuccess = false;
                failureReasons.append("상품 ").append(item.getProductId())
                        .append(": ").append(e.getMessage()).append("; ");
                log.error("[재고 복구 실패] orderId: {}, productId: {}, 에러: {}",
                        orderId, item.getProductId(), e.getMessage());
            }
        }

        // DB status 업데이트
        try {
            jdbcInventoryRepository.cancelByOrderId(orderId);
        } catch (Exception e) {
            log.error("[재고 취소 상태 업데이트 실패] orderId: {}, 에러: {}", orderId, e.getMessage());
        }

        if (allSuccess) {
            log.info("[재고 복구 완료] orderId: {}, salesId: {}", orderId, salesId);
            return ResReleaseStockDTO.success(orderId, salesId);
        } else {
            return ResReleaseStockDTO.failed(orderId, salesId, failureReasons.toString());
        }
    }
}

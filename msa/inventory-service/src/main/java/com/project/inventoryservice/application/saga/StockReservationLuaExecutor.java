package com.project.inventoryservice.application.saga;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 재고 예약 관련 Lua 스크립트 실행기
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockReservationLuaExecutor {

    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final String RESERVED_KEY_PREFIX = "reserved:";
    private static final String RESERVATION_KEY_PREFIX = "reservation:";

    private final RedissonClient redissonClient;

    private String reserveStockScript;
    private String confirmReservationScript;
    private String releaseReservationScript;

    @PostConstruct
    public void loadScripts() {
        try {
            reserveStockScript = loadLuaScript("lua/reserve_stock.lua");
            confirmReservationScript = loadLuaScript("lua/confirm_reservation.lua");
            releaseReservationScript = loadLuaScript("lua/release_reservation.lua");
            log.info("[Lua Script] 재고 예약 스크립트 로드 완료");
        } catch (IOException e) {
            log.error("[Lua Script] 스크립트 로드 실패", e);
            throw new RuntimeException("Failed to load Lua scripts", e);
        }
    }

    private String loadLuaScript(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 재고 예약
     * @return 남은 가용 재고 (>=0), -1: 재고 부족, -2: 이미 예약됨
     */
    public ReservationResult reserveStock(String sagaId, Long productId, Integer quantity,
                                          Long salesId, int ttlSeconds) {
        List<Object> keys = Arrays.asList(
                STOCK_KEY_PREFIX + productId,
                RESERVED_KEY_PREFIX + productId,
                RESERVATION_KEY_PREFIX + sagaId + ":" + productId
        );

        RScript script = redissonClient.getScript(LongCodec.INSTANCE);
        Long result = script.eval(
                RScript.Mode.READ_WRITE,
                reserveStockScript,
                RScript.ReturnType.INTEGER,
                keys,
                quantity, salesId.toString(), ttlSeconds
        );

        if (result == null) {
            log.error("[예약 실패] 스크립트 실행 실패 - sagaId: {}, productId: {}", sagaId, productId);
            return ReservationResult.error("Script execution failed");
        }

        if (result == -1) {
            log.warn("[예약 실패] 재고 부족 - sagaId: {}, productId: {}, 요청: {}",
                    sagaId, productId, quantity);
            return ReservationResult.insufficientStock(productId);
        }

        log.info("[예약 성공] sagaId: {}, productId: {}, 예약수량: {}, 남은가용재고: {}",
                sagaId, productId, quantity, result);
        return ReservationResult.success(productId, quantity, result.intValue());
    }

    /**
     * 재고 확정 (예약 → 실제 차감)
     * @return 현재 총 재고 (>=0), -1: 예약 없음, -2: 재고 불일치
     */
    public ConfirmResult confirmReservation(String sagaId, Long productId) {
        List<Object> keys = Arrays.asList(
                STOCK_KEY_PREFIX + productId,
                RESERVED_KEY_PREFIX + productId,
                RESERVATION_KEY_PREFIX + sagaId + ":" + productId
        );

        RScript script = redissonClient.getScript(LongCodec.INSTANCE);
        Long result = script.eval(
                RScript.Mode.READ_WRITE,
                confirmReservationScript,
                RScript.ReturnType.INTEGER,
                keys
        );

        if (result == null) {
            log.error("[확정 실패] 스크립트 실행 실패 - sagaId: {}, productId: {}", sagaId, productId);
            return ConfirmResult.error("Script execution failed");
        }

        if (result == -1) {
            log.warn("[확정 스킵] 예약 없음 (이미 확정 또는 TTL 만료) - sagaId: {}, productId: {}",
                    sagaId, productId);
            return ConfirmResult.notFound(productId);
        }

        if (result == -2) {
            log.error("[확정 실패] 재고 불일치 - sagaId: {}, productId: {}", sagaId, productId);
            return ConfirmResult.error("Stock mismatch");
        }

        log.info("[확정 성공] sagaId: {}, productId: {}, 현재재고: {}", sagaId, productId, result);
        return ConfirmResult.success(productId, result.intValue());
    }

    /**
     * orderId로 모든 예약 확정 (결제 완료 시 사용)
     */
    public void confirmAllReservations(String orderId) {
        String pattern = RESERVATION_KEY_PREFIX + orderId + ":*";

        redissonClient.getKeys().getKeysByPattern(pattern).forEach(key -> {
            // key format: reservation:{orderId}:{productId}
            String[] parts = key.split(":");
            if (parts.length >= 3) {
                try {
                    Long productId = Long.parseLong(parts[2]);
                    ConfirmResult result = confirmReservation(orderId, productId);
                    if (!result.success() && !"RESERVATION_NOT_FOUND".equals(result.errorReason())) {
                        log.error("[전체 확정 중 실패] orderId: {}, productId: {}, 사유: {}",
                                orderId, productId, result.errorReason());
                    }
                } catch (NumberFormatException e) {
                    log.warn("[전체 확정] productId 파싱 실패 - key: {}", key);
                }
            }
        });

        log.info("[전체 확정 완료] orderId: {}", orderId);
    }

    /**
     * 예약 해제 (보상 트랜잭션)
     * @return 해제된 수량 (>0), 0: 이미 해제됨 (멱등성)
     */
    public ReleaseResult releaseReservation(String sagaId, Long productId) {
        List<Object> keys = Arrays.asList(
                RESERVED_KEY_PREFIX + productId,
                RESERVATION_KEY_PREFIX + sagaId + ":" + productId
        );

        RScript script = redissonClient.getScript(LongCodec.INSTANCE);
        Long result = script.eval(
                RScript.Mode.READ_WRITE,
                releaseReservationScript,
                RScript.ReturnType.INTEGER,
                keys
        );

        if (result == null) {
            log.error("[해제 실패] 스크립트 실행 실패 - sagaId: {}, productId: {}", sagaId, productId);
            return ReleaseResult.error("Script execution failed");
        }

        if (result == 0) {
            log.info("[해제 스킵] 예약 없음 (이미 해제 또는 TTL 만료) - sagaId: {}, productId: {}",
                    sagaId, productId);
            return ReleaseResult.alreadyReleased(productId);
        }

        log.info("[해제 성공] sagaId: {}, productId: {}, 해제수량: {}", sagaId, productId, result);
        return ReleaseResult.success(productId, result.intValue());
    }

    // ==================== Result Classes ====================

    public record ReservationResult(
            boolean success,
            Long productId,
            Integer reservedQuantity,
            Integer availableStock,
            String errorReason
    ) {
        public static ReservationResult success(Long productId, Integer reserved, Integer available) {
            return new ReservationResult(true, productId, reserved, available, null);
        }

        public static ReservationResult insufficientStock(Long productId) {
            return new ReservationResult(false, productId, 0, 0, "INSUFFICIENT_STOCK");
        }

        public static ReservationResult error(String reason) {
            return new ReservationResult(false, null, 0, 0, reason);
        }
    }

    public record ConfirmResult(
            boolean success,
            Long productId,
            Integer currentStock,
            String errorReason
    ) {
        public static ConfirmResult success(Long productId, Integer currentStock) {
            return new ConfirmResult(true, productId, currentStock, null);
        }

        public static ConfirmResult notFound(Long productId) {
            return new ConfirmResult(false, productId, null, "RESERVATION_NOT_FOUND");
        }

        public static ConfirmResult error(String reason) {
            return new ConfirmResult(false, null, null, reason);
        }
    }

    public record ReleaseResult(
            boolean success,
            Long productId,
            Integer releasedQuantity,
            String errorReason
    ) {
        public static ReleaseResult success(Long productId, Integer released) {
            return new ReleaseResult(true, productId, released, null);
        }

        public static ReleaseResult alreadyReleased(Long productId) {
            return new ReleaseResult(true, productId, 0, null);  // 멱등성 - 성공으로 처리
        }

        public static ReleaseResult error(String reason) {
            return new ReleaseResult(false, null, 0, reason);
        }
    }
}

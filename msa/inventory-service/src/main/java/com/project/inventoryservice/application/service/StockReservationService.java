package com.project.inventoryservice.application.service;

import com.project.inventoryservice.application.response.ResConfirmStockDTO;
import com.project.inventoryservice.application.response.ResReleaseStockDTO;
import com.project.inventoryservice.application.response.ResReserveStockDTO;
import com.project.inventoryservice.application.saga.StockReservationLuaExecutor;
import com.project.inventoryservice.presentation.request.ReqConfirmStockDTO;
import com.project.inventoryservice.presentation.request.ReqReleaseStockDTO;
import com.project.inventoryservice.presentation.request.ReqReserveStockDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final StockReservationLuaExecutor luaExecutor;

    @Value("${inventory.reservation.ttl-seconds:900}")
    private int reservationTtlSeconds;

    /**
     * 재고 예약 (동기 API용)
     */
    public ResReserveStockDTO reserveStock(ReqReserveStockDTO request) {
        String orderId = request.getOrderId();
        Long salesId = request.getSalesId();
        List<ReqReserveStockDTO.ReserveItem> items = request.getItems();

        log.info("[재고 예약 시작] orderId: {}, salesId: {}, 상품 수: {}",
                orderId, salesId, items.size());

        List<ResReserveStockDTO.ItemResult> results = new ArrayList<>();
        List<Long> successfullyReserved = new ArrayList<>();
        boolean allSuccess = true;
        String failureReason = null;

        for (ReqReserveStockDTO.ReserveItem item : items) {
            StockReservationLuaExecutor.ReservationResult result = luaExecutor.reserveStock(
                    orderId,
                    item.getProductId(),
                    item.getQuantity(),
                    salesId,
                    reservationTtlSeconds
            );

            results.add(ResReserveStockDTO.ItemResult.builder()
                    .productId(item.getProductId())
                    .requestedQuantity(item.getQuantity())
                    .reservedQuantity(result.success() ? item.getQuantity() : 0)
                    .availableStock(result.availableStock())
                    .success(result.success())
                    .errorReason(result.errorReason())
                    .build());

            if (result.success()) {
                successfullyReserved.add(item.getProductId());
            } else {
                allSuccess = false;
                failureReason = "상품 " + item.getProductId() + ": " + result.errorReason();
                break;
            }
        }

        // 실패 시 이미 예약된 것들 롤백
        if (!allSuccess && !successfullyReserved.isEmpty()) {
            log.warn("[예약 롤백 시작] orderId: {}, 롤백 대상: {}", orderId, successfullyReserved);
            for (Long productId : successfullyReserved) {
                luaExecutor.releaseReservation(orderId, productId);
            }
        }

        if (allSuccess) {
            log.info("[재고 예약 완료] orderId: {}, salesId: {}", orderId, salesId);
            return ResReserveStockDTO.success(orderId, salesId, results);
        } else {
            log.warn("[재고 예약 실패] orderId: {}, salesId: {}, 사유: {}",
                    orderId, salesId, failureReason);
            return ResReserveStockDTO.failed(orderId, salesId, results, failureReason);
        }
    }

    /**
     * 재고 확정 (예약 → 실제 차감)
     */
    public ResConfirmStockDTO confirmStock(ReqConfirmStockDTO request) {
        String orderId = request.getOrderId();
        Long salesId = request.getSalesId();
        List<ReqConfirmStockDTO.ConfirmItem> items = request.getItems();

        log.info("[재고 확정 시작] orderId: {}, salesId: {}, 상품 수: {}",
                orderId, salesId, items.size());

        boolean allSuccess = true;
        StringBuilder failureReasons = new StringBuilder();

        for (ReqConfirmStockDTO.ConfirmItem item : items) {
            StockReservationLuaExecutor.ConfirmResult result =
                    luaExecutor.confirmReservation(orderId, item.getProductId());

            if (!result.success() && !"RESERVATION_NOT_FOUND".equals(result.errorReason())) {
                allSuccess = false;
                failureReasons.append("상품 ").append(item.getProductId())
                        .append(": ").append(result.errorReason()).append("; ");
                log.error("[재고 확정 실패] orderId: {}, productId: {}, 사유: {}",
                        orderId, item.getProductId(), result.errorReason());
            }
        }

        if (allSuccess) {
            log.info("[재고 확정 완료] orderId: {}, salesId: {}", orderId, salesId);
            return ResConfirmStockDTO.success(orderId, salesId);
        } else {
            return ResConfirmStockDTO.failed(orderId, salesId, failureReasons.toString());
        }
    }

    /**
     * 예약 해제 (보상 트랜잭션)
     */
    public ResReleaseStockDTO releaseStock(ReqReleaseStockDTO request) {
        String orderId = request.getOrderId();
        Long salesId = request.getSalesId();
        List<ReqReleaseStockDTO.ReleaseItem> items = request.getItems();

        log.info("[예약 해제 시작] orderId: {}, salesId: {}, 상품 수: {}, 사유: {}",
                orderId, salesId, items.size(), request.getReason());

        boolean allSuccess = true;
        StringBuilder failureReasons = new StringBuilder();

        for (ReqReleaseStockDTO.ReleaseItem item : items) {
            StockReservationLuaExecutor.ReleaseResult result =
                    luaExecutor.releaseReservation(orderId, item.getProductId());

            if (!result.success()) {
                allSuccess = false;
                failureReasons.append("상품 ").append(item.getProductId())
                        .append(": ").append(result.errorReason()).append("; ");
                log.error("[예약 해제 실패] orderId: {}, productId: {}, 사유: {}",
                        orderId, item.getProductId(), result.errorReason());
            }
        }

        if (allSuccess) {
            log.info("[예약 해제 완료] orderId: {}, salesId: {}", orderId, salesId);
            return ResReleaseStockDTO.success(orderId, salesId);
        } else {
            return ResReleaseStockDTO.failed(orderId, salesId, failureReasons.toString());
        }
    }
}

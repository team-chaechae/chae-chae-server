package com.project.inventoryservice.presentation.controller;

import com.project.inventoryservice.application.response.ResConfirmStockDTO;
import com.project.inventoryservice.application.response.ResReleaseStockDTO;
import com.project.inventoryservice.application.response.ResReserveStockDTO;
import com.project.inventoryservice.application.service.StockReservationService;
import com.project.inventoryservice.presentation.request.ReqConfirmStockDTO;
import com.project.inventoryservice.presentation.request.ReqReleaseStockDTO;
import com.project.inventoryservice.presentation.request.ReqReserveStockDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/reservation")
@Tag(name = "Stock Reservation API", description = "재고 예약/확정/해제 API")
public class StockReservationController {

    private final StockReservationService stockReservationService;

    @Operation(summary = "재고 예약", description = "주문에 대한 재고를 예약합니다 (TTL 적용)")
    @PostMapping("/reserve")
    public ResponseEntity<ResReserveStockDTO> reserveStock(
            @Valid @RequestBody ReqReserveStockDTO request) {
        log.debug("Stock reserve request: orderId={}, salesId={}",
                request.getOrderId(), request.getSalesId());

        ResReserveStockDTO response = stockReservationService.reserveStock(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "재고 확정", description = "예약된 재고를 확정합니다 (결제 완료 후 호출)")
    @PostMapping("/confirm")
    public ResponseEntity<ResConfirmStockDTO> confirmStock(
            @Valid @RequestBody ReqConfirmStockDTO request) {
        log.debug("Stock confirm request: orderId={}, salesId={}",
                request.getOrderId(), request.getSalesId());

        ResConfirmStockDTO response = stockReservationService.confirmStock(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "재고 예약 해제", description = "예약된 재고를 해제합니다 (결제 실패 시 롤백)")
    @PostMapping("/release")
    public ResponseEntity<ResReleaseStockDTO> releaseStock(
            @Valid @RequestBody ReqReleaseStockDTO request) {
        log.debug("Stock release request: orderId={}, salesId={}",
                request.getOrderId(), request.getSalesId());

        ResReleaseStockDTO response = stockReservationService.releaseStock(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}

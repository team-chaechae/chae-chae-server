package com.project.orderservice.infrastructure.client;

import com.project.orderservice.infrastructure.client.dto.InventoryChangeDTO;
import com.project.orderservice.infrastructure.client.dto.StockReservationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Inventory Service와 통신하기 위한 Feign Client
 */
@FeignClient(name = "inventory-service")
public interface InventoryClient {

    /**
     * 재고 증가 (주문 취소 시 재고 복구)
     */
    @PostMapping("/api/inventory/sales/increase")
    InventoryChangeDTO.Response increaseInventory(@RequestBody InventoryChangeDTO.Request request);

    /**
     * 재고 차감 (주문 승인 시 재고 감소)
     */
    @PostMapping("/api/inventory/sales/decrease")
    InventoryChangeDTO.Response decreaseInventory(@RequestBody InventoryChangeDTO.Request request);

    /**
     * 재고 예약 (주문 생성 시)
     */
    @PostMapping("/api/inventory/reservation/reserve")
    StockReservationDTO.ReserveResponse reserveStock(@RequestBody StockReservationDTO.ReserveRequest request);

    /**
     * 재고 확정 (결제 완료 후)
     */
    @PostMapping("/api/inventory/reservation/confirm")
    StockReservationDTO.ConfirmResponse confirmStock(@RequestBody StockReservationDTO.ConfirmRequest request);

    /**
     * 재고 예약 해제 (결제 실패 시 롤백)
     */
    @PostMapping("/api/inventory/reservation/release")
    StockReservationDTO.ReleaseResponse releaseStock(@RequestBody StockReservationDTO.ReleaseRequest request);
}

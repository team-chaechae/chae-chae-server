package com.project.orderservice.infrastructure.client;

import com.project.orderservice.infrastructure.client.dto.InventoryChangeDTO;
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
}

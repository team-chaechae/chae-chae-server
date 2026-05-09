package com.project.orderservice.infrastructure.client;

import com.project.orderservice.infrastructure.client.dto.InventoryChangeDTO;
import com.project.orderservice.infrastructure.client.dto.StockReservationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service")
public interface InventoryFeignClient {

    @PostMapping("/api/inventory/reservation/reserve")
    StockReservationDTO.ReserveResponse reserveStock(@RequestBody StockReservationDTO.ReserveRequest request);

    @PostMapping("/api/inventory/reservation/release")
    StockReservationDTO.ReleaseResponse releaseStock(@RequestBody StockReservationDTO.ReleaseRequest request);

    @PostMapping("/api/inventory/reservation/confirm")
    StockReservationDTO.ConfirmResponse confirmStock(@RequestBody StockReservationDTO.ConfirmRequest request);

    @PostMapping("/api/inventory/sales/increase")
    InventoryChangeDTO.Response increaseInventory(@RequestBody InventoryChangeDTO.Request request);

    @PostMapping("/api/inventory/sales/decrease")
    InventoryChangeDTO.Response decreaseInventory(@RequestBody InventoryChangeDTO.Request request);
}

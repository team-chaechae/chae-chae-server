package com.project.inventoryservice.presentation.controller.internal;

import com.project.inventoryservice.application.response.internal.ResInventoryChangeDTO;
import com.project.inventoryservice.application.service.internal.InventoryInternalService;
import com.project.inventoryservice.presentation.request.internal.ReqInventoryChangeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내부 서비스 호출용 재고 API
 * order-service 등 다른 MSA 서비스에서 호출
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/inventory/internal")
public class InventoryInternalController {

    private final InventoryInternalService inventoryInternalService;

    /**
     * 재고 증가 (주문 취소 시 재고 복구)
     */
    @PostMapping("/increase")
    public ResponseEntity<ResInventoryChangeDTO> increaseInventory(@RequestBody ReqInventoryChangeDTO dto) {
        log.info("[Internal API] 재고 증가 요청");
        return ResponseEntity.ok(inventoryInternalService.increaseInventory(dto));
    }

    /**
     * 재고 차감 (주문 승인 시 재고 감소)
     */
    @PostMapping("/decrease")
    public ResponseEntity<ResInventoryChangeDTO> decreaseInventory(@RequestBody ReqInventoryChangeDTO dto) {
        log.info("[Internal API] 재고 차감 요청");
        return ResponseEntity.ok(inventoryInternalService.decreaseInventory(dto));
    }
}

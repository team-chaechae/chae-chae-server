package com.project.inventoryservice.presentation.controller.sales;

import com.project.inventoryservice.application.response.sales.ResSalesInventoryDTO;
import com.project.inventoryservice.application.service.sales.SalesInventoryService;
import com.project.inventoryservice.presentation.request.sales.ReqSalesInventoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 판매 재고 API
 * 주문/판매에 따른 재고 증감 처리
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/inventory/sales")
public class SalesInventoryController {

    private final SalesInventoryService salesInventoryService;

    /**
     * 재고 증가 (주문 취소 시 재고 복구)
     */
    @PostMapping("/increase")
    public ResponseEntity<ResSalesInventoryDTO> increaseInventory(@RequestBody ReqSalesInventoryDTO dto) {
        log.info("[Sales API] 재고 증가 요청");
        return ResponseEntity.ok(salesInventoryService.increaseInventory(dto));
    }

    /**
     * 재고 차감 (주문 승인 시 재고 감소)
     */
    @PostMapping("/decrease")
    public ResponseEntity<ResSalesInventoryDTO> decreaseInventory(@RequestBody ReqSalesInventoryDTO dto) {
        log.info("[Sales API] 재고 차감 요청");
        return ResponseEntity.ok(salesInventoryService.decreaseInventory(dto));
    }
}

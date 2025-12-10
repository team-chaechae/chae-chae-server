package com.project.inventoryservice.presentation.controller.warehouse;

import com.project.inventoryservice.application.global.dto.ResDTO;
import com.project.inventoryservice.application.response.warehouse.ResWarehouseModifyDTO;
import com.project.inventoryservice.application.response.warehouse.ResWarehouseReceiveDTO;
import com.project.inventoryservice.application.response.warehouse.ResWarehouseShipmentDTO;
import com.project.inventoryservice.application.service.warehouse.WarehouseInventoryService;
import com.project.inventoryservice.presentation.controller.warehouse.docs.WarehouseInventoryControllerSwagger;
import com.project.inventoryservice.presentation.request.warehouse.ReqWarehouseModifyDTO;
import com.project.inventoryservice.presentation.request.warehouse.ReqWarehouseReceiveDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 물류/재고 관리 전용 컨트롤러
 *
 * 책임:
 * - 대량 입고 처리
 * - 물류 출고 처리
 * - 재고 조정 (단건/대량)
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/inventory/warehouse")
public class WarehouseInventoryController implements WarehouseInventoryControllerSwagger {

    private final WarehouseInventoryService warehouseInventoryService;

    @Override
    @PostMapping("/receive")
    public ResponseEntity<ResDTO<ResWarehouseReceiveDTO>> receiveInventory(
        @RequestBody ReqWarehouseReceiveDTO request) {

        log.info("[물류] 대량 재고 입고 요청: {} 건", request.getInventory().size());

        return new ResponseEntity<>(
            ResDTO.<ResWarehouseReceiveDTO>builder()
                .code(HttpStatus.CREATED.value())
                .message("재고 입고 완료")
                .data(warehouseInventoryService.receiveInventory(request))
                .build(),
            HttpStatus.CREATED
        );
    }

    @Override
    @PatchMapping("/modify")
    public ResponseEntity<ResDTO<ResWarehouseModifyDTO>> modifyInventory(
        @RequestBody ReqWarehouseModifyDTO request) {

        log.info("[물류] 재고 조정 요청: {} 건", request.getInventory().size());

        return new ResponseEntity<>(
            ResDTO.<ResWarehouseModifyDTO>builder()
                .code(HttpStatus.OK.value())
                .message("재고 조정 완료")
                .data(warehouseInventoryService.modifyInventory(request))
                .build(),
            HttpStatus.OK
        );
    }

    @Override
    @PatchMapping("/shipment")
    public ResponseEntity<ResDTO<ResWarehouseShipmentDTO>> shipInventory(
        @RequestBody ReqWarehouseModifyDTO request) {

        log.info("[물류] 대량 출고 요청: {} 건", request.getInventory().size());

        return new ResponseEntity<>(
            ResDTO.<ResWarehouseShipmentDTO>builder()
                .code(HttpStatus.OK.value())
                .message("물류 출고 완료")
                .data(warehouseInventoryService.shipInventory(request))
                .build(),
            HttpStatus.OK
        );
    }
}

package com.project.chaechaeserver.presentation.controller.inventory.bulk;

import static com.project.chaechaeserver.domain.model.user.constraint.RoleType.Role.ADMIN;

import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.inventory.bulk.ResBulkCreateInventoryPostDTO;
import com.project.chaechaeserver.application.response.inventory.bulk.ResBulkSaleInventoryDTO;
import com.project.chaechaeserver.application.response.inventory.bulk.ResUpdateInventoryDTO;
import com.project.chaechaeserver.application.service.inventory.bulk.InventoryBulkService;
import com.project.chaechaeserver.presentation.controller.inventory.bulk.docs.InventoryBulkControllerSwagger;
import com.project.chaechaeserver.presentation.request.inventory.bulk.ReqBulkCreateInventoryDTO;
import com.project.chaechaeserver.presentation.request.inventory.bulk.ReqUpdateInventoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
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
@RequestMapping("/api/inventory/bulk")
public class InventoryBulkController implements InventoryBulkControllerSwagger {

    private final InventoryBulkService inventoryBulkService;

    @Override
    @PostMapping
    @Secured(ADMIN)
    public ResponseEntity<ResDTO<ResBulkCreateInventoryPostDTO>> createInventory(
        @RequestBody ReqBulkCreateInventoryDTO request) {

        log.info("[물류] 대량 재고 입고 요청: {} 건", request.getInventory().size());

        return new ResponseEntity<>(
            ResDTO.<ResBulkCreateInventoryPostDTO>builder()
                .code(HttpStatus.CREATED.value())
                .message("재고 입고 완료")
                .data(inventoryBulkService.createInventory(request))
                .build(),
            HttpStatus.CREATED
        );
    }

    @Override
    @PatchMapping("/modify")
    @Secured(ADMIN)
    public ResponseEntity<ResDTO<ResUpdateInventoryDTO>> modifyInventory(
        @RequestBody ReqUpdateInventoryDTO request) {

        log.info("[물류] 재고 조정 요청: {} 건", request.getInventory().size());

        return new ResponseEntity<>(
            ResDTO.<ResUpdateInventoryDTO>builder()
                .code(HttpStatus.OK.value())
                .message("재고 조정 완료")
                .data(inventoryBulkService.modifyInventory(request))
                .build(),
            HttpStatus.OK
        );
    }

    @Override
    @PatchMapping("/warehouse/shipment")
    @Secured(ADMIN)
    public ResponseEntity<ResDTO<ResBulkSaleInventoryDTO>> decreaseInventoryForWarehouseShipment(
        @RequestBody ReqUpdateInventoryDTO request) {

        log.info("[물류] 대량 출고 요청: {} 건", request.getInventory().size());

        return new ResponseEntity<>(
            ResDTO.<ResBulkSaleInventoryDTO>builder()
                .code(HttpStatus.OK.value())
                .message("물류 출고 완료")
                .data(inventoryBulkService.decreaseInventoryForWarehouseShipment(request))
                .build(),
            HttpStatus.OK
        );
    }
}

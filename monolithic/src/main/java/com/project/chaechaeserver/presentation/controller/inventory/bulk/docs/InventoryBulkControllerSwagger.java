package com.project.chaechaeserver.presentation.controller.inventory.bulk.docs;

import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.inventory.bulk.ResBulkCreateInventoryPostDTO;
import com.project.chaechaeserver.application.response.inventory.bulk.ResBulkSaleInventoryDTO;
import com.project.chaechaeserver.application.response.inventory.bulk.ResUpdateInventoryDTO;
import com.project.chaechaeserver.presentation.request.inventory.bulk.ReqBulkCreateInventoryDTO;
import com.project.chaechaeserver.presentation.request.inventory.bulk.ReqUpdateInventoryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Inventory Bulk", description = "물류/재고 관리 전용 API를 제공합니다.")
@RequestMapping("/api/inventory/bulk")
public interface InventoryBulkControllerSwagger {

    @Operation(summary = "대량 재고 입고", description = "여러 상품의 재고를 한번에 입고하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "재고 입고 성공", content = @Content(schema = @Schema(implementation = ResBulkCreateInventoryPostDTO.class))),
            @ApiResponse(responseCode = "400", description = "재고 입고 실패", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @PostMapping
    ResponseEntity<ResDTO<ResBulkCreateInventoryPostDTO>> createInventory(@RequestBody ReqBulkCreateInventoryDTO request);

    @Operation(summary = "재고 조정", description = "단일 또는 여러 상품의 재고를 조정하는 API입니다. 단건/대량 모두 지원합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재고 조정 성공", content = @Content(schema = @Schema(implementation = ResUpdateInventoryDTO.class))),
            @ApiResponse(responseCode = "400", description = "재고 조정 실패", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @PatchMapping("/modify")
    ResponseEntity<ResDTO<ResUpdateInventoryDTO>> modifyInventory(@RequestBody ReqUpdateInventoryDTO request);

    @Operation(summary = "물류 출고", description = "물류팀 전용 대량 출고 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "출고 성공", content = @Content(schema = @Schema(implementation = ResBulkSaleInventoryDTO.class))),
            @ApiResponse(responseCode = "400", description = "출고 실패", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @PatchMapping("/warehouse/shipment")
    ResponseEntity<ResDTO<ResBulkSaleInventoryDTO>> decreaseInventoryForWarehouseShipment(@RequestBody ReqUpdateInventoryDTO request);
}
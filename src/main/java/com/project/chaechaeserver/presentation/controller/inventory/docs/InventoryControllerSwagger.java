    package com.project.chaechaeserver.presentation.controller.inventory.docs;

import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.inventory.ResGetInventoryDTO;
import com.project.chaechaeserver.application.response.inventory.ResInventorySearchDTO;
import com.project.chaechaeserver.application.response.inventory.ResSingleUpdateInventoryDTO;
import com.project.chaechaeserver.application.response.inventory.ResVulkCreateInventoryPostDTO;
import com.project.chaechaeserver.application.response.inventory.ResVulkUpdateInventoryDTO;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import com.project.chaechaeserver.presentation.request.inventory.ReqSingleUpdateInventoryDTO;
import com.project.chaechaeserver.presentation.request.inventory.ReqVulkCreateInventoryDTO;
import com.project.chaechaeserver.presentation.request.inventory.ReqVulkUpdateInventoryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Inventory", description = "재고 관리 관련 API를 제공합니다.")
@RequestMapping("/api/inventory")
public interface InventoryControllerSwagger {

    @Operation(summary = "벌크 재고 생성", description = "여러 상품의 재고를 한번에 생성하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "벌크 재고 생성 성공", content = @Content(schema = @Schema(implementation = ResVulkCreateInventoryPostDTO.class))),
            @ApiResponse(responseCode = "400", description = "벌크 재고 생성 실패", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @PostMapping
    ResponseEntity<ResDTO<ResVulkCreateInventoryPostDTO>> createInventoryForBulk(@RequestBody ReqVulkCreateInventoryDTO request);

    @Operation(summary = "단건 재고 수정", description = "단일 상품의 재고를 수정하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재고 수정 성공", content = @Content(schema = @Schema(implementation = ResSingleUpdateInventoryDTO.class))),
            @ApiResponse(responseCode = "400", description = "재고 수정 실패", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @PatchMapping("/modify")
    ResponseEntity<ResDTO<ResSingleUpdateInventoryDTO>> modifyInventoryForSingle(@RequestBody ReqSingleUpdateInventoryDTO request);

    @Operation(summary = "벌크 재고 수정", description = "여러 상품의 재고를 한번에 수정하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "벌크 재고 수정 성공", content = @Content(schema = @Schema(implementation = ResVulkUpdateInventoryDTO.class))),
            @ApiResponse(responseCode = "400", description = "벌크 재고 수정 실패", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @PatchMapping("/bulk/modify")
    ResponseEntity<ResDTO<ResVulkUpdateInventoryDTO>> modifyInventoryForBulk(@RequestBody ReqVulkUpdateInventoryDTO request);

    @Operation(summary = "재고 상세조회", description = "재고 ID로 재고 정보를 상세조회하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재고 조회 성공", content = @Content(schema = @Schema(implementation = ResGetInventoryDTO.class))),
            @ApiResponse(responseCode = "400", description = "재고 조회 실패", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @GetMapping("/{inventoryId}")
    ResponseEntity<ResDTO<ResGetInventoryDTO>> getInventory(@PathVariable Long inventoryId);

    @Operation(summary = "재고 검색", description = "다양한 조건으로 재고를 검색하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재고 검색 성공", content = @Content(schema = @Schema(implementation = ResInventorySearchDTO.class))),
            @ApiResponse(responseCode = "400", description = "재고 검색 실패", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @GetMapping
    ResponseEntity<ResDTO<ResInventorySearchDTO>> searchInventoryByCondition(
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) ProductStatusType status,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate exactDate,
            @RequestParam(required = false) List<String> sort,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable);
}
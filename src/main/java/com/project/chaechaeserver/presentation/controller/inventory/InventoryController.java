package com.project.chaechaeserver.presentation.controller.inventory;


import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.inventory.ResGetInventoryDTO;
import com.project.chaechaeserver.application.response.inventory.ResInventorySearchDTO;
import com.project.chaechaeserver.application.response.inventory.ResSingleUpdateInventoryDTO;
import com.project.chaechaeserver.application.response.inventory.ResVulkCreateInventoryPostDTO;
import com.project.chaechaeserver.application.response.inventory.ResVulkUpdateInventoryDTO;
import com.project.chaechaeserver.application.service.inventory.InventoryService;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import com.project.chaechaeserver.presentation.request.inventory.ReqSingleUpdateInventoryDTO;
import com.project.chaechaeserver.presentation.request.inventory.ReqVulkCreateInventoryDTO;
import com.project.chaechaeserver.presentation.request.inventory.ReqVulkUpdateInventoryDTO;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping()
    public ResponseEntity<ResDTO<ResVulkCreateInventoryPostDTO>> createInventoryForBulk(
        @RequestBody ReqVulkCreateInventoryDTO request) {

        log.info("벌크 재고 생성 요청: {}", request);

        return new ResponseEntity<>(
            ResDTO.<ResVulkCreateInventoryPostDTO>builder()
                .code(HttpStatus.CREATED.value())
                .message("벌크 재고 생성 완료")
                .data(inventoryService.createInventoryForVulk(request))
                .build(),
            HttpStatus.CREATED
        );
    }


    @PatchMapping("/modify")
    public ResponseEntity<ResDTO<ResSingleUpdateInventoryDTO>> modifyInventoryForSingle(
        @RequestBody ReqSingleUpdateInventoryDTO request) {

        log.info("단건 재고 수정 요청: {}", request);

        return new ResponseEntity<>(
            ResDTO.<ResSingleUpdateInventoryDTO>builder()
                .code(HttpStatus.OK.value())
                .message("재고 수정 완료")
                .data(inventoryService.modifyInventoryForSingle(request))
                .build(),
            HttpStatus.OK
        );
    }


    @PatchMapping("/bulk/modify")
    public ResponseEntity<ResDTO<ResVulkUpdateInventoryDTO>> modifyInventoryForBulk(
        @RequestBody ReqVulkUpdateInventoryDTO request) {

        log.info("벌크 재고 수정 요청: {}", request);

        return new ResponseEntity<>(
            ResDTO.<ResVulkUpdateInventoryDTO>builder()
                .code(HttpStatus.OK.value())
                .message("벌크 재고 수정 완료")
                .data(inventoryService.modifyInventoryForVulk(request))
                .build(),
            HttpStatus.OK
        );

    }


    @GetMapping("/{inventoryId}")
    public ResponseEntity<ResDTO<ResGetInventoryDTO>> getInventory(@PathVariable Long inventoryId) {

        return new ResponseEntity<>(
            ResDTO.<ResGetInventoryDTO>builder()
                .code(HttpStatus.OK.value())
                .message("상품 검색에 성공하였습니다")
                .data(inventoryService.getInventoryInfo(inventoryId))
                .build(),
            HttpStatus.OK
        );
    }

    @GetMapping
    public ResponseEntity<ResDTO<ResInventorySearchDTO>> searchInventoryByCondition(
        @RequestParam(required = false) Boolean deleted,
        @RequestParam(required = false) String productName,
        @RequestParam(required = false) ProductStatusType status,
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate exactDate,
        @RequestParam(required = false) List<String> sort,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return new ResponseEntity<>(
            ResDTO.<ResInventorySearchDTO>builder()
                .code(HttpStatus.OK.value())
                .message("상품 검색에 성공하였습니다")
                .data(inventoryService.getInventorySearchInfo(pageable, productName, deleted,status, productId,
                    startDate, endDate, exactDate, sort))
                .build(),
            HttpStatus.OK
        );
    }
}
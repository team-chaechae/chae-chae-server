package com.project.inventoryservice.presentation.controller.query;

import com.project.inventoryservice.application.global.dto.ResDTO;
import com.project.inventoryservice.application.response.ResGetInventoryDTO;
import com.project.inventoryservice.application.response.ResInventorySearchDTO;
import com.project.inventoryservice.application.service.query.InventoryQueryService;
import com.project.inventoryservice.presentation.controller.query.docs.InventoryQueryControllerSwagger;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 재고 조회 컨트롤러
 *
 * 책임:
 * - 재고 상세 조회
 * - 재고 검색/목록 조회
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/inventory")
public class InventoryQueryController implements InventoryQueryControllerSwagger {

    private final InventoryQueryService inventoryQueryService;

    @Override
    @GetMapping("/{inventoryId}")
    public ResponseEntity<ResDTO<ResGetInventoryDTO>> getInventory(@PathVariable Long inventoryId) {

        log.info("재고 상세 조회 요청: inventoryId={}", inventoryId);

        return new ResponseEntity<>(
            ResDTO.<ResGetInventoryDTO>builder()
                .code(HttpStatus.OK.value())
                .message("재고 조회 완료")
                .data(inventoryQueryService.getInventoryInfo(inventoryId))
                .build(),
            HttpStatus.OK
        );
    }

    @Override
    @GetMapping
    public ResponseEntity<ResDTO<ResInventorySearchDTO>> searchInventoryByCondition(
        @RequestParam(required = false) Boolean deleted,
        @RequestParam(required = false) String productName,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate exactDate,
        @RequestParam(required = false) List<String> sort,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("재고 검색 요청");

        return new ResponseEntity<>(
            ResDTO.<ResInventorySearchDTO>builder()
                .code(HttpStatus.OK.value())
                .message("재고 검색 완료")
                .data(inventoryQueryService.getInventorySearchInfo(pageable, productName, deleted,
                    status, productId, startDate, endDate, exactDate, sort))
                .build(),
            HttpStatus.OK
        );
    }
}

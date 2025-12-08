package com.project.orderservice.presentation.controller.docs;

import com.project.orderservice.application.global.dto.ResDTO;
import com.project.orderservice.application.response.ResSalesCreateDTO;
import com.project.orderservice.application.response.ResSalesGetByIdDTO;
import com.project.orderservice.application.response.ResSalesSearchDTO;
import com.project.orderservice.presentation.request.ReqCreateSalesDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Sales", description = "판매(사용자 주문) 관련 API를 제공합니다.")
@RequestMapping("/api/sales")
public interface SalesControllerSwagger {

    @Operation(summary = "판매 생성", description = "상품 판매를 생성하고 재고를 차감합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "판매 생성 성공", content = @Content(schema = @Schema(implementation = ResSalesCreateDTO.class))),
            @ApiResponse(responseCode = "400", description = "재고 부족 또는 잘못된 요청", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @PostMapping
    ResponseEntity<ResDTO<ResSalesCreateDTO>> createSales(@Valid @RequestBody ReqCreateSalesDTO dto);

    @Operation(summary = "판매기록 상세조회", description = "판매기록을 상세조회하는 API 입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상세조회 성공", content = @Content(schema = @Schema(implementation = ResSalesGetByIdDTO.class))),
            @ApiResponse(responseCode = "400", description = "상세조회 실패.", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @GetMapping("/{salesId}")
    ResponseEntity<ResDTO<ResSalesGetByIdDTO>> getSalesBySalesId(@PathVariable Long salesId);

    @Operation(summary = "판매기록 검색", description = "판매기록을 검색하는 API 입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공", content = @Content(schema = @Schema(implementation = ResSalesSearchDTO.class))),
            @ApiResponse(responseCode = "400", description = "검색 실패.", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @GetMapping
    ResponseEntity<ResDTO<ResSalesSearchDTO>> searchSalesByCondition(
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate exactDate,
            @RequestParam(required = false) List<String> sort,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );
}

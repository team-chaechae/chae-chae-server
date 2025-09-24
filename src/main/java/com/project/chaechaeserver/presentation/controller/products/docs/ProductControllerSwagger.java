package com.project.chaechaeserver.presentation.controller.products.docs;

import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.products.ResCreateProductPostDTO;
import com.project.chaechaeserver.application.response.products.ResGetProductWithOrderStatus;
import com.project.chaechaeserver.application.response.products.ResProductSearchWithOrderStatusDTO;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import com.project.chaechaeserver.presentation.request.products.ReqCreateProductsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Products", description = "상품 관리 관련 API를 제공합니다.")
@RequestMapping("/api/products")
public interface ProductControllerSwagger {

    @Operation(
            summary = "상품 생성", 
            description = "새로운 상품을 생성하는 API입니다. (관리자 권한 필요)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "상품 생성 성공", content = @Content(schema = @Schema(implementation = ResCreateProductPostDTO.class))),
            @ApiResponse(responseCode = "400", description = "상품 생성 실패", content = @Content(schema = @Schema(implementation = ResDTO.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @PostMapping
    ResponseEntity<ResDTO<ResCreateProductPostDTO>> createProduct(@RequestBody ReqCreateProductsDTO dto);

    @Operation(summary = "상품 상세조회", description = "상품 ID로 상품 정보를 상세조회하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상품 조회 성공", content = @Content(schema = @Schema(implementation = ResGetProductWithOrderStatus.class))),
            @ApiResponse(responseCode = "400", description = "상품 조회 실패", content = @Content(schema = @Schema(implementation = ResDTO.class))),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @GetMapping("/{productId}")
    ResponseEntity<ResDTO<ResGetProductWithOrderStatus>> getProduct(@PathVariable Long productId);

    @Operation(summary = "상품 검색", description = "다양한 조건으로 상품을 검색하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상품 검색 성공", content = @Content(schema = @Schema(implementation = ResProductSearchWithOrderStatusDTO.class))),
            @ApiResponse(responseCode = "400", description = "상품 검색 실패", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @GetMapping
    ResponseEntity<ResDTO<ResProductSearchWithOrderStatusDTO>> searchProductByCondition(
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) ProductStatusType status,
            @RequestParam(required = false) ProductStatusType.ProductOrderType orderStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate exactDate,
            @RequestParam(required = false) List<String> sort,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable);
}
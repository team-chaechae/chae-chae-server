package com.project.productservice.presentation.controller;

import com.project.productservice.application.global.dto.ResDTO;
import com.project.productservice.application.response.ResCreateProductPostDTO;
import com.project.productservice.application.response.ResGetProductWithOrderStatus;
import com.project.productservice.application.response.ResProductSearchWithOrderStatusDTO;
import com.project.productservice.application.service.ProductsService;
import com.project.productservice.domain.model.constraint.ProductStatusType;
import com.project.productservice.presentation.request.ReqCreateProductsDTO;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/products")
public class ProductController {

    private final ProductsService productsService;

    @PostMapping()
    public ResponseEntity<ResDTO<ResCreateProductPostDTO>> createProduct(
        @RequestBody ReqCreateProductsDTO dto) {

        return new ResponseEntity<>(
            ResDTO.<ResCreateProductPostDTO>builder()
                .code(HttpStatus.CREATED.value())
                .message("상품 생성 완료")
                .data(productsService.createProductInfo(dto))
                .build(),
                HttpStatus.CREATED
        );
    }
    @GetMapping("/{productId}")
    public ResponseEntity<ResDTO<ResGetProductWithOrderStatus>> getProduct(@PathVariable Long productId) {
        log.info("상품 조회 요청: productId={}", productId);

        return new ResponseEntity<>(ResDTO.<ResGetProductWithOrderStatus>builder()
            .code(HttpStatus.OK.value())
            .message("상품 조회 완료")
            .data(productsService.getProductInfo(productId))
            .build(),
            HttpStatus.OK
        );

    }

    @GetMapping
    public ResponseEntity<ResDTO<ResProductSearchWithOrderStatusDTO>> searchProductByCondition(
        @RequestParam(required = false) Boolean deleted,
        @RequestParam(required = false)String productName,
        @RequestParam(required = false)ProductStatusType status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate exactDate,
        @RequestParam(required = false) List<String> sort,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return new ResponseEntity<>(
            ResDTO.<ResProductSearchWithOrderStatusDTO>builder()
                .code(HttpStatus.OK.value())
                .message("상품 검색에 성공하였습니다")
                .data(productsService.getProductSearchInfo(pageable, productName, deleted, status,
                   startDate, endDate, exactDate, sort))
                .build(),
            HttpStatus.OK
        );
    }

    /**
     * 상품 ID 목록 유효성 검증 API (inventory-service에서 호출)
     */
    @PostMapping("/validate")
    public ResponseEntity<List<Long>> validateProductIds(@RequestBody List<Long> productIds) {
        return ResponseEntity.ok(productsService.validateProductIds(productIds));
    }

    /**
     * 내부 서비스용 상품 정보 조회 API (order-service에서 호출)
     */
    @GetMapping("/{productId}/internal")
    public ResponseEntity<com.project.productservice.application.response.internal.ProductInternalDTO> getProductForInternal(@PathVariable Long productId) {
        return ResponseEntity.ok(productsService.getProductForInternal(productId));
    }

    /**
     * 내부 서비스용 상품 정보 배치 조회 API (order-service에서 호출)
     */
    @PostMapping("/internal/batch")
    public ResponseEntity<java.util.Map<Long, com.project.productservice.application.response.internal.ProductInternalDTO>> getProductsForInternal(@RequestBody List<Long> productIds) {
        return ResponseEntity.ok(productsService.getProductsForInternal(productIds));
    }
}

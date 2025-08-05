package com.project.chaechaeserver.presentation.controller.products;

import static com.project.chaechaeserver.domain.model.user.constraint.RoleType.Role.ADMIN;

import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.products.ResCreateProductPostDTO;
import com.project.chaechaeserver.application.response.products.ResGetProductWithOrderStatus;
import com.project.chaechaeserver.application.response.products.ResProductSearchWithOrderStatusDTO;
import com.project.chaechaeserver.application.service.products.ProductsService;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import com.project.chaechaeserver.presentation.request.products.ReqCreateProductsDTO;
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
import org.springframework.security.access.annotation.Secured;
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
    @Secured(ADMIN)
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
        @RequestParam(required = false)ProductStatusType.ProductOrderType orderStatus ,
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
                    orderStatus,  startDate, endDate, exactDate, sort))
                .build(),
            HttpStatus.OK
        );
    }
}

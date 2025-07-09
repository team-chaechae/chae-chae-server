package com.project.chaechaeserver.presentation.controller.products;

import static com.project.chaechaeserver.domain.model.user.constraint.RoleType.Role.ADMIN;

import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.products.ResCreateProductPostDTO;
import com.project.chaechaeserver.application.response.products.ResGetProductDTO;
import com.project.chaechaeserver.application.service.products.ProductsService;
import com.project.chaechaeserver.presentation.request.products.ReqCreateProductsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<ResDTO<ResGetProductDTO>> getProduct(@PathVariable Long productId) {
        log.info("상품 조회 요청: productId={}", productId);

        return new ResponseEntity<>(ResDTO.<ResGetProductDTO>builder()
            .code(HttpStatus.OK.value())
            .message("상품 조회 완료")
            .data(productsService.getProductInfo(productId))
            .build(),
            HttpStatus.OK
        );


    }
}

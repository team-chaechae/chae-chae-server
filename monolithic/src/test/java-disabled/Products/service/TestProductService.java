package com.project.chaechaeserver.Products.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;


import com.project.chaechaeserver.application.service.products.ProductsServiceImpl;
import com.project.chaechaeserver.domain.service.products.ProductDomainService;
import com.project.chaechaeserver.presentation.request.products.ReqCreateProductsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class TestProductService {




    @InjectMocks
    private ProductsServiceImpl productsService;

    private ReqCreateProductsDTO validRequest;
    @Mock
    private ProductDomainService productDomainService;




    @BeforeEach
        // 테스트 전에 데이터 초기화
    void setUp() {
        validRequest = createValidRequest();
    }


    @Test
    @DisplayName("중복 상품명으로 상품 생성 실패")
    void createProductInfo_DuplicateName_ThrowsException() {

        willThrow(new IllegalArgumentException("이미 존재하는 상품명입니다: 사과"))
            .given(productDomainService)
            .validateProductName("사과");

        assertThatThrownBy(() -> productsService.createProductInfo(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이미 존재하는 상품명입니다: 사과");

        verify(productDomainService).validateProductName("사과");
    }

    private ReqCreateProductsDTO createValidRequest() {
        ReqCreateProductsDTO.Product product = ReqCreateProductsDTO.Product.builder()
            .name("사과")
            .category("과일")
            .price(3000)
            .unit("개")
            .build();

        return ReqCreateProductsDTO.builder()
            .product(product)
            .build();
    }
}

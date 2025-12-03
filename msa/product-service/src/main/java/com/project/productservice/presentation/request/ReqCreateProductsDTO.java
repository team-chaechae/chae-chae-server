package com.project.productservice.presentation.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ReqCreateProductsDTO {

    private Product product;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Product {

        private String name;
        private String category;
        private Integer price;
    }
}

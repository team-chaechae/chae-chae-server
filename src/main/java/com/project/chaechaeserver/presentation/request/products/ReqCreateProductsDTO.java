package com.project.chaechaeserver.presentation.request.products;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

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
        private int price;
        private String unit;


    }
}

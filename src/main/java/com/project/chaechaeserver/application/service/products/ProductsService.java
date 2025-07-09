package com.project.chaechaeserver.application.service.products;


import com.project.chaechaeserver.application.response.products.ResCreateProductPostDTO;
import com.project.chaechaeserver.application.response.products.ResGetProductDTO;
import com.project.chaechaeserver.presentation.request.products.ReqCreateProductsDTO;

public interface ProductsService {

    ResCreateProductPostDTO createProductInfo(ReqCreateProductsDTO request);
    ResGetProductDTO getProductInfo(Long productId);
}

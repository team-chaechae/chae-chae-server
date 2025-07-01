package com.project.chaechaeserver.application.service.products;

import com.project.chaechaeserver.application.response.products.ResCreateProductInfoDTO;
import com.project.chaechaeserver.presentation.request.products.ReqCreateProductsDTO;

public interface ProductsService {

    ResCreateProductInfoDTO createProductInfo(ReqCreateProductsDTO request);
}

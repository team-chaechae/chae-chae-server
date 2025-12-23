package com.project.chaechaeserver.application.service.products;


import com.project.chaechaeserver.application.response.products.ResCreateProductPostDTO;
import com.project.chaechaeserver.application.response.products.ResGetProductWithOrderStatus;
import com.project.chaechaeserver.application.response.products.ResProductSearchWithOrderStatusDTO;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import com.project.chaechaeserver.presentation.request.products.ReqCreateProductsDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ProductsService {

    ResCreateProductPostDTO createProductInfo(ReqCreateProductsDTO request);
    ResGetProductWithOrderStatus getProductInfo(Long productId);
    ResProductSearchWithOrderStatusDTO getProductSearchInfo(
        Pageable pageable,
        String productName, Boolean deletedAt, ProductStatusType productStatus, ProductStatusType.ProductOrderType orderStatus , LocalDate startDate,
        LocalDate endDate, LocalDate exactDate
        , List<String> sortList);
}

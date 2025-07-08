package com.project.chaechaeserver.application.service.products;

import com.project.chaechaeserver.application.response.products.ResCreateProductPostDTO;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
import com.project.chaechaeserver.domain.service.products.ProductDomainService;
import com.project.chaechaeserver.presentation.request.products.ReqCreateProductsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductsServiceImpl implements ProductsService {

    private final ProductDomainService productDomainService;
    private final ProductsRepository productsRepository;


    @Override
    @Transactional
    public ResCreateProductPostDTO createProductInfo(ReqCreateProductsDTO request) {

        productDomainService.validateProductName(request.getProduct().getName());

        ProductEntity savedProduct = productsRepository.save(
            ProductEntity.createProducts(
                request.getProduct().getName(),
                request.getProduct().getCategory(),
                request.getProduct().getPrice(),
                request.getProduct().getUnit()
            )
        );

        return ResCreateProductPostDTO.from(
        savedProduct
        );
    }
}

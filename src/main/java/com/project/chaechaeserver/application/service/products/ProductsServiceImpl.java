package com.project.chaechaeserver.application.service.products;

import com.project.chaechaeserver.application.response.products.ResCreateProductPostDTO;
import com.project.chaechaeserver.application.response.products.ResGetProductDTO;
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
    public ResCreateProductPostDTO createProductInfo(ReqCreateProductsDTO dto) {

        productDomainService.validateProductName(dto.getProduct().getName());

        ProductEntity savedProduct = productsRepository.save(
            ProductEntity.createProducts(
                dto.getProduct().getName(),
                dto.getProduct().getCategory(),
                dto.getProduct().getPrice(),
                dto.getProduct().getUnit()
            )
        );

        return ResCreateProductPostDTO.from(
        savedProduct
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResGetProductDTO getProductInfo(Long productId) {
        ProductEntity product = productsRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        return ResGetProductDTO.from(product);
    }
}


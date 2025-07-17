package com.project.chaechaeserver.domain.service.products;

import com.project.chaechaeserver.domain.model.products.ProductEntity;

public interface ProductDomainService {

    void validateProductName(String name);

    ProductEntity findProductById(Long id);

}

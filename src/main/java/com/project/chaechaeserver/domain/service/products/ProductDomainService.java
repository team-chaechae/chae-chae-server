package com.project.chaechaeserver.domain.service.products;

import com.project.chaechaeserver.domain.model.products.ProductEntity;
import java.util.List;

public interface ProductDomainService {

    void validateProductName(String name);

    ProductEntity findProductById(Long id);

    List<ProductEntity> validateProductIds(List<Long> productId);
}

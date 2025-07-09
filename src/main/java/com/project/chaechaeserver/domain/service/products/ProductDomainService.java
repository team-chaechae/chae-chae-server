package com.project.chaechaeserver.domain.service.products;

public interface ProductDomainService {

    void validateProductName(String name);

    // 발주용 상품 가격 확인
    int getUnitPrice(Long productId);
}

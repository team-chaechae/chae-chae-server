package com.project.chaechaeserver.domain.service.products;

import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductDomainServiceImpl implements ProductDomainService {

    private final ProductsRepository productsRepository;

    @Override
    public void validateProductName(String name) {
        if (productsRepository.existsByName(name)) {
            throw new IllegalArgumentException("이미 존재하는 상품명입니다: " + name);
        }
    }

    // 발주용 상품 가격 확인
    @Override
    public int getUnitPrice(Long productId) {
        return productsRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."))
            .getPrice();
    }
}

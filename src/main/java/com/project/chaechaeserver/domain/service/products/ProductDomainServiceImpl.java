package com.project.chaechaeserver.domain.service.products;

import com.project.chaechaeserver.application.global.excepion.EntityAlreadyExistException;
import com.project.chaechaeserver.application.global.excepion.EntityNotFoundException;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductDomainServiceImpl implements ProductDomainService {

    private final ProductsRepository productsRepository;

    @Override
    public void validateProductName(String name) {
        if (productsRepository.existsByProductName(name)) {
            throw new EntityAlreadyExistException("이미 존재하는 상품명입니다: " + name);
        }
    }

    @Override
    public ProductEntity findProductById(Long id) {
        return productsRepository.findProductByProductId(id);
    }

    @Override
    public List<ProductEntity> validateProductIds(List<Long> productIds) {

        List<ProductEntity> foundProducts = productsRepository.findAllByIdInForWrite(productIds);

        List<Long> foundsIds = foundProducts.stream().map(ProductEntity::getId).toList();

        List<Long> missingIds = foundsIds.stream().filter(id -> !foundsIds.contains(id)).toList();

        if (!missingIds.isEmpty()) {
            throw new EntityNotFoundException("존재하지 않는 상품 ID: " + missingIds);
        }

        return foundProducts;
    }
}
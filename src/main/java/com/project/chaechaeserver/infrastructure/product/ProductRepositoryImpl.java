package com.project.chaechaeserver.infrastructure.product;

import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductsRepository {

    private final ProductQueryRepository productQueryRepository;
    private final JpaProductRepository jpaProductRepository;

    @Override
    public boolean existsByProductName(String name) {
        return jpaProductRepository.existsByNameAndDeletedAtIsNull(name);
    }

    @Override
    public ProductEntity save(ProductEntity productEntity) {
        return jpaProductRepository.save(productEntity);
    }

    @Override
    public ProductEntity findByIdForUpdate(Long id) {
        return jpaProductRepository.findByIdForUpdate(id).orElseThrow(() -> new IllegalArgumentException("상푸을 찾을 수 없습니다"));
    }


    @Override
    public ProductEntity findProductByProductId(Long id) {
        return jpaProductRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다"));
    }
    @Override
    public Page<ProductEntity> findProductByDeletedAtIsNullWithCondition(Pageable pageable,
        String productName, Boolean deletedAt, ProductStatusType productStatus,ProductStatusType.ProductOrderType orderStatus, LocalDate startDate,
        LocalDate endDate, LocalDate exactDate
        , List<String> sortList) {
        return productQueryRepository.findProductByDeletedAtIsNullWithCondition(
            pageable, productName, deletedAt, productStatus, orderStatus, startDate, endDate, exactDate, sortList);
    }

}

package com.project.productservice.infrastructure;

import com.project.productservice.application.global.exception.EntityNotFoundException;
import com.project.productservice.domain.model.ProductEntity;
import com.project.productservice.domain.model.constraint.ProductStatusType;
import com.project.productservice.domain.repository.ProductsRepository;
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
    public List<ProductEntity> findAllByIdInForWrite(List<Long> ids) {
        return jpaProductRepository.findAllByIdInForWrite(ids);
    }

    @Override
    public ProductEntity findByIdForUpdate(Long id) {
        return jpaProductRepository.findByIdForUpdate(id).orElseThrow(() -> new EntityNotFoundException("유효하지 않은 상품 정보입니다."));
    }


    @Override
    public ProductEntity findProductByProductId(Long id) {
        return jpaProductRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 상품 정보입니다."));
    }
    @Override
    public Page<ProductEntity> findProductByDeletedAtIsNullWithCondition(Pageable pageable,
        String productName, Boolean deletedAt, ProductStatusType productStatus, LocalDate startDate,
        LocalDate endDate, LocalDate exactDate
        , List<String> sortList) {
        return productQueryRepository.findProductByDeletedAtIsNullWithCondition(
            pageable, productName, deletedAt, productStatus, startDate, endDate, exactDate, sortList);
    }

    @Override
    public List<ProductEntity> findAllById(List<Long> testProductIds) {
        return jpaProductRepository.findAllById(testProductIds);
    }

    @Override
    public List<ProductEntity> findAll() {
        return jpaProductRepository.findAll();
    }

}

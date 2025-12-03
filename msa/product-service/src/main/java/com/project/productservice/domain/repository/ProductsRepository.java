package com.project.productservice.domain.repository;

import com.project.productservice.domain.model.ProductEntity;
import com.project.productservice.domain.model.constraint.ProductStatusType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductsRepository  {

    boolean existsByProductName(String name);

    ProductEntity save(ProductEntity productEntity);

    List<ProductEntity> findAllByIdInForWrite(List<Long> ids);

    ProductEntity findByIdForUpdate(Long id);

    ProductEntity findProductByProductId(Long id);

    Page<ProductEntity> findProductByDeletedAtIsNullWithCondition(Pageable pageable,
        String productName, Boolean deletedAt, ProductStatusType productStatus , LocalDate startDate,
        LocalDate endDate, LocalDate exactDate
        , List<String> sortList);

    List<ProductEntity> findAllById(List<Long> testProductIds);

    List<ProductEntity> findAll();
}

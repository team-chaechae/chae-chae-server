package com.project.chaechaeserver.domain.repository.products;

import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
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
        String productName, Boolean deletedAt, ProductStatusType productStatus,ProductStatusType.ProductOrderType orderStatus , LocalDate startDate,
        LocalDate endDate, LocalDate exactDate
        , List<String> sortList);


}

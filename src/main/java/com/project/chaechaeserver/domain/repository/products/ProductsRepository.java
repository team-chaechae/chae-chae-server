package com.project.chaechaeserver.domain.repository.products;

import com.project.chaechaeserver.domain.model.products.ProductsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductsRepository extends JpaRepository<ProductsEntity, Long> {

    boolean existsByName(String name);
}

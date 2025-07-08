package com.project.chaechaeserver.domain.repository.products;

import com.project.chaechaeserver.domain.model.products.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductsRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsByName(String name);
}

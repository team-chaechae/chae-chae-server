package com.project.chaechaeserver.infrastructure.sales;

import com.project.chaechaeserver.domain.model.sales.SalesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSalesRepository extends JpaRepository<SalesEntity, Long> {
}

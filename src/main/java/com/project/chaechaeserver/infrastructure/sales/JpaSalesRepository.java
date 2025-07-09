package com.project.chaechaeserver.infrastructure.sales;

import com.project.chaechaeserver.domain.model.sales.SalesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaSalesRepository extends JpaRepository<SalesEntity, Long> {

    Optional<SalesEntity> findByIdAndDeletedAtIsNull(Long salesId);

}

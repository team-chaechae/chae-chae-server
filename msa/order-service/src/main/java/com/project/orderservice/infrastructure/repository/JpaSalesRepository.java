package com.project.orderservice.infrastructure.repository;

import com.project.orderservice.domain.model.SalesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaSalesRepository extends JpaRepository<SalesEntity, Long> {

    Optional<SalesEntity> findByIdAndDeletedAtIsNull(Long salesId);
}

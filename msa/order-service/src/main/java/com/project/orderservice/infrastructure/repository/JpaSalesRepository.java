package com.project.orderservice.infrastructure.repository;

import com.project.orderservice.domain.model.SalesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaSalesRepository extends JpaRepository<SalesEntity, Long> {

    Optional<SalesEntity> findByIdAndDeletedAtIsNull(Long salesId);

    @Query("SELECT s FROM SalesEntity s LEFT JOIN FETCH s.items " +
           "WHERE s.id = :salesId AND s.deletedAt IS NULL")
    Optional<SalesEntity> findByIdWithItemsAndDeletedAtIsNull(@Param("salesId") Long salesId);
}

package com.project.productservice.infrastructure;

import com.project.productservice.domain.model.ProductEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaProductRepository extends JpaRepository<ProductEntity, Long> {


    Optional<ProductEntity> findByIdAndDeletedAtIsNull(Long productId);

    @Query("SELECT p FROM ProductEntity p WHERE p.id IN (:ids)")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ProductEntity> findAllByIdInForWrite(@Param("ids") List<Long> ids);

    boolean existsByNameAndDeletedAtIsNull(String productName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.id = :id")
    Optional<ProductEntity> findByIdForUpdate(@Param("id") Long id);

}

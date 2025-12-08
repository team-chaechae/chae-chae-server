package com.project.inventoryservice.domain.repository;

import com.project.inventoryservice.domain.model.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StockRepository extends JpaRepository<StockEntity, Long> {

    Optional<StockEntity> findByProductId(Long productId);

    List<StockEntity> findByProductIdIn(List<Long> productIds);

    /**
     * 재고 증가
     */
    @Modifying
    @Query("UPDATE StockEntity s SET s.quantity = s.quantity + :amount WHERE s.productId = :productId")
    int increaseStock(@Param("productId") Long productId, @Param("amount") int amount);

    /**
     * 재고 감소
     */
    @Modifying
    @Query("UPDATE StockEntity s SET s.quantity = s.quantity - :amount WHERE s.productId = :productId")
    int decreaseStock(@Param("productId") Long productId, @Param("amount") int amount);

    /**
     * 재고가 충분한 경우에만 감소 (과잉판매 방지)
     */
    @Modifying
    @Query("UPDATE StockEntity s SET s.quantity = s.quantity - :amount WHERE s.productId = :productId AND s.quantity >= :amount")
    int decreaseStockIfEnough(@Param("productId") Long productId, @Param("amount") int amount);
}

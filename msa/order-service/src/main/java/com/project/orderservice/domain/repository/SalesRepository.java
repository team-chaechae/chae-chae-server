package com.project.orderservice.domain.repository;

import com.project.orderservice.domain.model.SalesEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface SalesRepository {

    SalesEntity save(SalesEntity salesEntity);

    SalesEntity findSalesBySalesId(Long salesId);

    Page<SalesEntity> findSalesByDeletedAtIsNullWithCondition(
            Pageable pageable,
            Boolean deletedCond,
            String productName,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate exactDate,
            List<String> sortList
    );
}

package com.project.productservice.domain.repository.sales;

import com.project.productservice.domain.model.sales.SalesEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface SalesRepository {

    SalesEntity findSalesBySalesId(Long salesId);
    Page<SalesEntity> findSalesByDeletedAtIsNullWithCondition(Pageable pageable, Boolean deletedCond, String productName,
                                                                     LocalDate startDate, LocalDate endDate, LocalDate exactDate,
                                                                     List<String> sortList);

}

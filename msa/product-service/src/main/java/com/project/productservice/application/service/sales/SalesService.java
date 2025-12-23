package com.project.productservice.application.service.sales;

import com.project.productservice.application.response.sales.ResSalesGetByIdDTO;
import com.project.productservice.application.response.sales.ResSalesSearchDTO;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface SalesService {

    ResSalesGetByIdDTO getSalesBySalesId(Long salesId);

    ResSalesSearchDTO searchSalesByCondition(Pageable pageable, Boolean deletedCond, String productName, LocalDate startDate, LocalDate endDate, LocalDate exactDate, List<String> sortList);
}

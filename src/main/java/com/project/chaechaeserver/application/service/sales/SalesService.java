package com.project.chaechaeserver.application.service.sales;

import com.project.chaechaeserver.application.response.sales.ResSalesGetByIdDTO;
import com.project.chaechaeserver.application.response.sales.ResSalesSearchDTO;
import com.project.chaechaeserver.domain.repository.sales.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final SalesRepository salesRepository;

    @Transactional(readOnly = true)
    public ResSalesGetByIdDTO getSalesBySalesId(Long salesId) {
        return ResSalesGetByIdDTO.of(
                salesRepository.getSalesBySalesId(salesId)
        );
    }

    @Transactional(readOnly = true)
    public ResSalesSearchDTO searchSalesByCondition(Pageable pageable, Boolean deletedCond, String productName,
                                                    LocalDate startDate, LocalDate endDate, LocalDate exactDate,
                                                    List<String> sortList) {

        return ResSalesSearchDTO.of(
                salesRepository.findSalesByDeletedAtIsNullWithCondition(
                        pageable, deletedCond, productName, startDate, endDate, exactDate, sortList
                )
        );
    }
}
package com.project.productservice.application.service.sales;

import com.project.productservice.application.response.sales.ResSalesGetByIdDTO;
import com.project.productservice.application.response.sales.ResSalesSearchDTO;
import com.project.productservice.domain.repository.sales.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private final SalesRepository salesRepository;

    @Override
    @Transactional(readOnly = true)
    public ResSalesGetByIdDTO getSalesBySalesId(Long salesId) {
        return ResSalesGetByIdDTO.from(
                salesRepository.findSalesBySalesId(salesId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResSalesSearchDTO searchSalesByCondition(Pageable pageable, Boolean deletedCond, String productName,
                                                    LocalDate startDate, LocalDate endDate, LocalDate exactDate,
                                                    List<String> sortList) {

        return ResSalesSearchDTO.from(
                salesRepository.findSalesByDeletedAtIsNullWithCondition(
                        pageable, deletedCond, productName, startDate, endDate, exactDate, sortList
                )
        );
    }
}

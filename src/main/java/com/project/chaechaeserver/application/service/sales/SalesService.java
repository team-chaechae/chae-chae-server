package com.project.chaechaeserver.application.service.sales;

import com.project.chaechaeserver.application.response.sales.ResSalesGetByIdDTO;
import com.project.chaechaeserver.domain.repository.sales.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
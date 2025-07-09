package com.project.chaechaeserver.infrastructure.sales;

import com.project.chaechaeserver.domain.model.sales.SalesEntity;
import com.project.chaechaeserver.domain.repository.sales.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SalesRepositoryImpl implements SalesRepository {

    private final JpaSalesRepository jpaSalesRepository;
    private final SalesQueryRepository salesQueryRepository;

    @Override
    public SalesEntity findSalesBySalesId(Long salesId) {
        return jpaSalesRepository.findByIdAndDeletedAtIsNull(salesId)
                .orElseThrow(() -> new IllegalArgumentException("유요하지 않은 판매 정보입니다."));
    }

    @Override
    public Page<SalesEntity> findSalesByDeletedAtIsNullWithCondition(Pageable pageable, Boolean deletedCond, String productName,
                                                                     LocalDate startDate, LocalDate endDate, LocalDate exactDate,
                                                                     List<String> sortList) {
        return salesQueryRepository.findSalesByDeletedAtIsNullWithCondition(
                pageable, deletedCond, productName, startDate, endDate, exactDate, sortList
        );
    }
}

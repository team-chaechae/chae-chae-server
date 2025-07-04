package com.project.chaechaeserver.infrastructure.sales;

import com.project.chaechaeserver.domain.model.sales.SalesEntity;
import com.project.chaechaeserver.domain.repository.sales.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SalesRepositoryImpl implements SalesRepository {

    private final JpaSalesRepository jpaSalesRepository;

    @Override
    public SalesEntity getSalesBySalesId(Long salesId) {
        return jpaSalesRepository.findByIdAndDeletedAtIsNull(salesId)
                .orElseThrow(() -> new IllegalArgumentException("유요하지 않은 판매 정보입니다."));
    }
}

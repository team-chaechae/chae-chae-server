package com.project.chaechaeserver.infrastructure.sales;

import com.project.chaechaeserver.domain.repository.sales.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SalesRepositoryImpl implements SalesRepository {

    private final JpaSalesRepository jpaSalesRepository;

}

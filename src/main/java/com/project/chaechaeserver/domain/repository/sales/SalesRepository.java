package com.project.chaechaeserver.domain.repository.sales;

import com.project.chaechaeserver.domain.model.sales.SalesEntity;

public interface SalesRepository {

    SalesEntity getSalesBySalesId(Long salesId);

}

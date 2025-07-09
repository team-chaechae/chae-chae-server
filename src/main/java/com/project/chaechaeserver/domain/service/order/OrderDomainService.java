package com.project.chaechaeserver.domain.service.order;

public interface OrderDomainService {
  void validateDuplicateOrder(Long productId);
}

package com.project.chaechaeserver.domain.service.order;

import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import com.project.chaechaeserver.domain.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderDomainServiceImpl implements OrderDomainService {

  private final OrderRepository orderRepository;

  @Override
  public void validateDuplicateOrder(Long productId) {
    if (orderRepository.existsByProductIdAndStatus(productId, StatusType.APPROVED)) {
      throw new IllegalArgumentException("이미 발주 중인 상품입니다.");
    }
  }

}

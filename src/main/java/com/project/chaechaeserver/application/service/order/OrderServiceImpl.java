package com.project.chaechaeserver.application.service.order;

import com.project.chaechaeserver.application.response.order.ResCreateOrderPostDTO;
import com.project.chaechaeserver.domain.model.order.OrderEntity;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import com.project.chaechaeserver.domain.repository.order.OrderRepository;
import com.project.chaechaeserver.domain.service.order.OrderDomainService;
import com.project.chaechaeserver.domain.service.products.ProductDomainService;
import com.project.chaechaeserver.presentation.request.order.ReqCreateOrderDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;
  private final OrderDomainService orderDomainService;
  private final ProductDomainService productDomainService;

  @Override
  @Transactional
  public ResCreateOrderPostDTO createOrderInfo(ReqCreateOrderDTO request) {
    Long productId = request.getOrder().getProductId();
    int quantity = request.getOrder().getQuantity();

    if (quantity <= 0) {
      throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
    }

    orderDomainService.validateDuplicateOrder(productId);

    int unitCost = productDomainService.getUnitPrice(productId);
    StatusType status = StatusType.REQUESTED;

    OrderEntity order = OrderEntity.createOrder(productId, quantity, unitCost, status);

    OrderEntity savedOrder = orderRepository.save(order);

    return ResCreateOrderPostDTO.of(
        ResCreateOrderPostDTO.OrderInfo.from(savedOrder)
    );
  }
}

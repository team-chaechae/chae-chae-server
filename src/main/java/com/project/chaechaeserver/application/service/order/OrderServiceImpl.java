package com.project.chaechaeserver.application.service.order;

import com.project.chaechaeserver.application.response.order.ResCreateOrderPostDTO;
import com.project.chaechaeserver.domain.model.order.OrderEntity;
import com.project.chaechaeserver.domain.model.order.ProductInfo;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
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
  public ResCreateOrderPostDTO createOrderInfo(ReqCreateOrderDTO dto) {
    Long productId = dto.getOrder().getProductId();
    Integer quantity = dto.getOrder().getQuantity();

    orderDomainService.validateDuplicateOrder(productId);

    StatusType status = StatusType.DEFAULT;

    ProductEntity product = productDomainService.findProductById(productId);

    ProductInfo productInfo = new ProductInfo(
        product.getId(),
        product.getName(),
        product.getCategory(),
        product.getPrice()
    );

    OrderEntity order = OrderEntity.createOrder(productInfo, quantity, status);

    OrderEntity savedOrder = orderRepository.save(order);

    return ResCreateOrderPostDTO.from(savedOrder);
  }
}

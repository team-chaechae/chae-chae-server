package com.project.chaechaeserver.application.service.order;

import com.project.chaechaeserver.application.response.order.ResCreateOrderPostDTO;
import com.project.chaechaeserver.application.response.order.ResOrdersSearchDTO;
import com.project.chaechaeserver.application.response.order.ResUpdateOrderDTO;
import com.project.chaechaeserver.domain.model.order.OrderEntity;
import com.project.chaechaeserver.domain.model.order.ProductInfo;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.repository.order.OrderRepository;
import com.project.chaechaeserver.domain.service.order.OrderDomainService;
import com.project.chaechaeserver.domain.service.products.ProductDomainService;
import com.project.chaechaeserver.presentation.request.order.ReqCreateOrderDTO;
import com.project.chaechaeserver.presentation.request.order.ReqUpdateOrderDTO;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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

    @Override
    @Transactional(readOnly = true)
    public ResOrdersSearchDTO searchOrdersByFilter(Pageable pageable, Long orderId,
                                                   String productName, String productCategory, StatusType status,
                                                   String createdBy, LocalDate startDate, LocalDate endDate,
                                                   List<String> sortList) {
        return ResOrdersSearchDTO.from(
                orderRepository.searchOrdersByFilter(
                        pageable, orderId, productName, productCategory, status, createdBy, startDate, endDate, sortList
                )
        );
    }

    @Override
    @Transactional
    public ResUpdateOrderDTO updateOrderStatus(ReqUpdateOrderDTO dto, Long orderId) {

        OrderEntity order = orderDomainService.findById(orderId);
        StatusType status = dto.getStatus();

        orderDomainService.validateAlreadyCompleted(order, status);

        order.updateStatus(status);

        if (status == StatusType.COMPLETED) {
            Long productId = order.getProductInfo().getProductId();
            Integer quantity = order.getQuantity();
            ProductEntity product = productDomainService.findProductById(productId);
            product.addInventory(quantity);
        }

        return ResUpdateOrderDTO.from(order);

    }

}

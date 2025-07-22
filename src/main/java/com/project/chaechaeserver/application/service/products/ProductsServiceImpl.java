package com.project.chaechaeserver.application.service.products;

import com.project.chaechaeserver.application.response.products.ResCreateProductPostDTO;
import com.project.chaechaeserver.application.response.products.ResGetProductWithOrderStatus;
import com.project.chaechaeserver.application.response.products.ResProductSearchWithOrderStatusDTO;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import com.project.chaechaeserver.domain.repository.order.OrderRepository;
import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
import com.project.chaechaeserver.domain.service.products.ProductDomainService;
import com.project.chaechaeserver.presentation.request.products.ReqCreateProductsDTO;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductsServiceImpl implements ProductsService {

    private final ProductDomainService productDomainService;
    private final ProductsRepository productsRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public ResCreateProductPostDTO createProductInfo(ReqCreateProductsDTO dto) {

        productDomainService.validateProductName(dto.getProduct().getName());

        ProductEntity savedProduct = productsRepository.save(
            ProductEntity.createProducts(
                dto.getProduct().getName(),
                dto.getProduct().getCategory(),
                dto.getProduct().getPrice()
            )
        );

        return ResCreateProductPostDTO.from(
        savedProduct
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResGetProductWithOrderStatus getProductInfo(Long productId) {

        ProductEntity product = productsRepository.findProductByProductId(productId);

        return ResGetProductWithOrderStatus.from(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ResProductSearchWithOrderStatusDTO getProductSearchInfo(Pageable pageable,
        String productName, Boolean deletedAt, ProductStatusType productStatus,ProductStatusType.ProductOrderType orderStatus , LocalDate startDate,
        LocalDate endDate, LocalDate exactDate
        , List<String> sortList) {
        return ResProductSearchWithOrderStatusDTO.from(productsRepository.findProductByDeletedAtIsNullWithCondition(pageable,
             productName,  deletedAt,  productStatus, orderStatus,  startDate,
             endDate,  exactDate
            , sortList));
    }

}


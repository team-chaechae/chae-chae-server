//package com.project.chaechaeserver.Products.service;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//
//import ch.qos.logback.classic.Logger;
//import com.project.chaechaeserver.application.response.products.ResGetProductWithOrderStatus;
//import com.project.chaechaeserver.application.service.products.ProductsService;
//import com.project.chaechaeserver.domain.repository.order.OrderRepository;
//import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.util.StopWatch;
//
//@SpringBootTest
//public class ProductRepositoryImplIntegrationTest {
//
//
//    @Autowired
//    ProductsService productsService;
//    @Autowired
//    OrderRepository orderRepository;
//    @Autowired
//    ProductsRepository productsRepository;
//
//    private Long orderId;
//    @BeforeEach
//    void setUp() {
//      orderId = 1L;
//    }
//
//    @Test
//    @DisplayName("주문 id로 상품 조회 테스트")
//    void getProductWithOrderStatus() {
//
//        StopWatch stopWatch = new StopWatch();
//        stopWatch.start();
//
//        ResGetProductWithOrderStatus result = productsService.getProductInfo(orderId);
//
//        stopWatch.stop();
//
//        assertThat(result).isNotNull();
//        System.out.println("execute time :" + stopWatch.getTotalTimeMillis());
//        System.out.println("Product ID: " + result.getProduct().getId());
//        System.out.println("Product Name: " + result.getProduct().getName());
//        System.out.println("Product Category: " + result.getProduct().getCategory());
//        System.out.println("Product Price: " + result.getProduct().getPrice());
//        System.out.println("Product Quantity: " + result.getProduct().getCurrentQuantity());
//        if (result.getOrder() != null) {
//            System.out.println("Order Status: " + result.getOrder().getStatus());
//        } else {
//            System.out.println("Order Status: null (승인되지 않은 주문)");
//        }
//    }
//
//}
//

package com.project.orderservice.infrastructure.client;

import com.project.orderservice.infrastructure.client.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/products/{productId}/internal")
    ProductDTO getProductById(@PathVariable("productId") Long productId);
}

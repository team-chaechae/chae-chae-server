package com.project.orderservice.infrastructure.client;

import com.project.orderservice.infrastructure.client.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/products/{productId}/internal")
    ProductDTO getProductById(@PathVariable("productId") Long productId);

    @PostMapping("/api/products/internal/batch")
    Map<Long, ProductDTO> getProductsByIds(@RequestBody List<Long> productIds);
}

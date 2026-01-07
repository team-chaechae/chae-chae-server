package com.project.inventoryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableCaching
@EnableRetry
@EnableScheduling
@EnableFeignClients
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.project.inventoryservice.domain.model",
        "com.project.common.dlq.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.project.inventoryservice.domain.repository",
        "com.project.inventoryservice.infrastructure.repository",
        "com.project.common.dlq.domain"
})
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}

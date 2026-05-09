package com.project.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDiscoveryClient
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@EntityScan(basePackages = {
        "com.project.paymentservice.domain.model",
        "com.project.common.dlq.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.project.paymentservice.domain.repository",
        "com.project.paymentservice.infrastructure.repository",
        "com.project.common.dlq.domain"
})
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

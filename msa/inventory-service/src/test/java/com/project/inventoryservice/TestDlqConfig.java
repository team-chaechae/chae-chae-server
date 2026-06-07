package com.project.inventoryservice;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 테스트용 DLQ 핸들러 빈 제공.
 */
@Configuration
@Profile("test")
public class TestDlqConfig {
}

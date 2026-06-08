-- order-service payment orchestration state table
-- Apply to order_db before disabling Hibernate ddl-auto in production.

CREATE TABLE IF NOT EXISTS payment_orchestrations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id VARCHAR(100) NOT NULL,
    sales_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    inventory_deducted TINYINT(1) NOT NULL DEFAULT 0,
    inventory_restored TINYINT(1) NOT NULL DEFAULT 0,
    payment_refunded TINYINT(1) NOT NULL DEFAULT 0,
    order_completed TINYINT(1) NOT NULL DEFAULT 0,
    order_cancelled TINYINT(1) NOT NULL DEFAULT 0,
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY idx_payment_orchestration_sales_id (sales_id),
    KEY idx_payment_orchestration_status (status)
);

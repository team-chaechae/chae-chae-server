-- payment-service Toss operation recovery table
-- Apply to payment_db before disabling Hibernate ddl-auto in production.

CREATE TABLE IF NOT EXISTS payment_toss_operations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operation_id VARCHAR(100) NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    order_id VARCHAR(100) NOT NULL,
    sales_id BIGINT NOT NULL,
    amount INT NULL,
    toss_payment_key VARCHAR(200) NOT NULL,
    payment_method VARCHAR(50) NULL,
    approved_at DATETIME(6) NULL,
    reason VARCHAR(500) NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY idx_payment_toss_operation_id (operation_id),
    KEY idx_payment_toss_operation_status (status)
);

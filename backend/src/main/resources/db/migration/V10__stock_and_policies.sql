-- Distributor stock model: per-product on-hand, reserved and low-stock threshold.
-- available = stock_qty - reserved_qty (computed at read time).
ALTER TABLE products
    ADD COLUMN stock_qty            DECIMAL(12,3) NOT NULL DEFAULT 0,
    ADD COLUMN reserved_qty         DECIMAL(12,3) NOT NULL DEFAULT 0,
    ADD COLUMN low_stock_threshold  DECIMAL(12,3) NOT NULL DEFAULT 0,
    ADD COLUMN track_stock          BOOLEAN NOT NULL DEFAULT FALSE;

-- Configurable per-distributor ordering policies.
CREATE TABLE distributor_policies (
    id                    BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    supplier_id           BIGINT UNSIGNED NOT NULL,
    out_of_stock_behavior VARCHAR(20) NOT NULL DEFAULT 'REJECT',  -- REJECT | BACKORDER | PARTIAL
    min_order_value       DECIMAL(12,2) NOT NULL DEFAULT 0,
    min_order_qty         DECIMAL(12,3) NOT NULL DEFAULT 0,
    auto_cancel_hours     INT NOT NULL DEFAULT 0,                 -- 0 = never auto-cancel
    enforce_credit_limit  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_policy_supplier (supplier_id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

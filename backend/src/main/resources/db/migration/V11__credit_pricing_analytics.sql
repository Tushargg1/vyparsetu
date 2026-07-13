-- Extend distributor policies with credit-action, large-order alert and approval gating.
ALTER TABLE distributor_policies
    ADD COLUMN credit_over_limit_action       VARCHAR(20)   NOT NULL DEFAULT 'BLOCK',  -- BLOCK | REQUIRE_APPROVAL
    ADD COLUMN large_order_threshold          DECIMAL(12,2) NOT NULL DEFAULT 0,        -- 0 = disabled
    ADD COLUMN allow_ordering_without_approval BOOLEAN      NOT NULL DEFAULT TRUE;

-- Customer-specific price overrides (price engine). Falls back to product default when absent.
CREATE TABLE customer_prices (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT UNSIGNED NOT NULL,
    retailer_id BIGINT UNSIGNED NOT NULL,
    product_id  BIGINT UNSIGNED NOT NULL,
    unit_price  DECIMAL(12,2) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_customer_price (supplier_id, retailer_id, product_id),
    KEY idx_customer_price_lookup (supplier_id, retailer_id, product_id, active),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE CASCADE,
    FOREIGN KEY (retailer_id) REFERENCES retailers(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Append-only analytics events powering dashboards and AI recommendations.
CREATE TABLE analytics_events (
    id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    supplier_id   BIGINT UNSIGNED NOT NULL,
    retailer_id   BIGINT UNSIGNED,
    event_type    VARCHAR(40) NOT NULL,
    product_id    BIGINT UNSIGNED,
    numeric_value DECIMAL(14,3),
    text_value    VARCHAR(512),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_analytics_supplier_type (supplier_id, event_type),
    KEY idx_analytics_product (supplier_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Retailer's own retail price per product
CREATE TABLE retail_prices (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    retailer_id BIGINT UNSIGNED NOT NULL,
    product_id  BIGINT UNSIGNED NOT NULL,
    price       DECIMAL(12,2) NOT NULL,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_retail_price (retailer_id, product_id),
    FOREIGN KEY (retailer_id) REFERENCES retailers(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Point-of-sale: a counter sale to an end customer
CREATE TABLE customer_sales (
    id           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    retailer_id  BIGINT UNSIGNED NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    total_items  DECIMAL(12,3) NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (retailer_id) REFERENCES retailers(id) ON DELETE CASCADE,
    INDEX idx_sale_retailer_date (retailer_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE customer_sale_items (
    id           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    sale_id      BIGINT UNSIGNED NOT NULL,
    product_id   BIGINT UNSIGNED NOT NULL,
    product_name VARCHAR(180) NOT NULL,
    quantity     DECIMAL(12,3) NOT NULL,
    unit_price   DECIMAL(12,2) NOT NULL,
    cost_price   DECIMAL(12,2),
    line_total   DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (sale_id) REFERENCES customer_sales(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_sale_item_sale (sale_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

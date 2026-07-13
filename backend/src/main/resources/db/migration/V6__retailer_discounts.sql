-- Reusable discount presets a retailer can apply to MRP when setting rates
CREATE TABLE retailer_discounts (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    retailer_id BIGINT UNSIGNED NOT NULL,
    label       VARCHAR(80) NOT NULL,
    percent     DECIMAL(5,2) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (retailer_id) REFERENCES retailers(id) ON DELETE CASCADE,
    INDEX idx_discount_retailer (retailer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Product alias engine: map retailer slang/abbreviations to catalog products
-- (e.g. "Coke" -> Coca-Cola) to reduce ambiguity and unnecessary LLM calls.
CREATE TABLE product_aliases (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT UNSIGNED NOT NULL,
    alias       VARCHAR(120) NOT NULL,
    product_id  BIGINT UNSIGNED NULL,
    canonical   VARCHAR(160) NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_alias (supplier_id, alias),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Conversation history + webhook idempotency. A NULL provider_message_id is
-- allowed (simulator); duplicate non-null ids are rejected by the unique key.
CREATE TABLE whatsapp_messages (
    id                  BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    supplier_id         BIGINT UNSIGNED NOT NULL,
    phone               VARCHAR(20) NOT NULL,
    direction           VARCHAR(10) NOT NULL,
    body                TEXT,
    provider_message_id VARCHAR(128) NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_provider_msg (provider_message_id),
    INDEX idx_wamsg (supplier_id, phone, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

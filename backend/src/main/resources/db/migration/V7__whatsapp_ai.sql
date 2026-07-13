-- WhatsApp AI: per-distributor connection + AI settings, customer onboarding
-- requests, multiple linked numbers per retailer, and pending order confirmations.

CREATE TABLE whatsapp_settings (
    id                   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    supplier_id          BIGINT UNSIGNED NOT NULL,
    connected            BOOLEAN NOT NULL DEFAULT FALSE,
    business_number      VARCHAR(20),
    ai_enabled           BOOLEAN NOT NULL DEFAULT TRUE,
    auto_reply           BOOLEAN NOT NULL DEFAULT TRUE,
    auto_create_orders   BOOLEAN NOT NULL DEFAULT FALSE,
    require_confirmation  BOOLEAN NOT NULL DEFAULT TRUE,
    human_takeover       BOOLEAN NOT NULL DEFAULT FALSE,
    seller_approval_mode VARCHAR(20) NOT NULL DEFAULT 'FIRST_ORDER_ONLY',
    language             VARCHAR(10) NOT NULL DEFAULT 'BOTH',
    business_hours_start VARCHAR(5) NOT NULL DEFAULT '09:00',
    business_hours_end   VARCHAR(5) NOT NULL DEFAULT '20:00',
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_wa_settings_supplier (supplier_id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE retailer_requests (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT UNSIGNED NOT NULL,
    shop_name   VARCHAR(180),
    gst_number  VARCHAR(20),
    address     VARCHAR(255),
    owner_name  VARCHAR(120),
    phone       VARCHAR(20) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message     VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE CASCADE,
    INDEX idx_req_supplier_status (supplier_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE retailer_phones (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    retailer_id BIGINT UNSIGNED NOT NULL,
    phone       VARCHAR(20) NOT NULL,
    verified    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_retailer_phone (phone),
    FOREIGN KEY (retailer_id) REFERENCES retailers(id) ON DELETE CASCADE,
    INDEX idx_phone_retailer (retailer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE whatsapp_pending_orders (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT UNSIGNED NOT NULL,
    retailer_id BIGINT UNSIGNED NOT NULL,
    phone       VARCHAR(20) NOT NULL,
    raw_text    VARCHAR(1000) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_pending_phone (phone),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE CASCADE,
    FOREIGN KEY (retailer_id) REFERENCES retailers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

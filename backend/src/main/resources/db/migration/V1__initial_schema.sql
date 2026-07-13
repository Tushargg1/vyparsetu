-- ============ IDENTITY & USERS ============
CREATE TABLE users (
    id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    uuid          CHAR(36) NOT NULL UNIQUE,
    name          VARCHAR(120) NOT NULL,
    phone         VARCHAR(15)  NOT NULL UNIQUE,
    email         VARCHAR(150) UNIQUE,
    password_hash VARCHAR(255),
    preferred_language ENUM('hi','en') NOT NULL DEFAULT 'en',
    status        ENUM('ACTIVE','SUSPENDED','PENDING') NOT NULL DEFAULT 'PENDING',
    phone_verified TINYINT(1) NOT NULL DEFAULT 0,
    email_verified TINYINT(1) NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP NULL,
    INDEX idx_users_phone (phone),
    INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE roles (
    id   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name ENUM('RETAILER','SUPPLIER','DELIVERY_PARTNER','ADMIN') NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_roles (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE otp_tokens (
    id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED,
    identifier VARCHAR(150) NOT NULL,
    channel    ENUM('SMS','EMAIL') NOT NULL,
    code_hash  VARCHAR(255) NOT NULL,
    purpose    ENUM('LOGIN','REGISTER','RESET') NOT NULL,
    attempts   INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_otp_identifier (identifier),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refresh_tokens (
    id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    device_info VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_refresh_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============ ROLE PROFILES ============
CREATE TABLE retailers (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL UNIQUE,
    shop_name   VARCHAR(150) NOT NULL,
    gst_number  VARCHAR(15),
    address     VARCHAR(255),
    city        VARCHAR(80),
    state       VARCHAR(80),
    pincode     VARCHAR(10),
    credit_approved TINYINT(1) NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE suppliers (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL UNIQUE,
    business_name VARCHAR(150) NOT NULL,
    supplier_type ENUM('DISTRIBUTOR','WHOLESALER','SUPER_STOCKIST') NOT NULL,
    gst_number  VARCHAR(15),
    address     VARCHAR(255),
    city        VARCHAR(80),
    state       VARCHAR(80),
    pincode     VARCHAR(10),
    whatsapp_enabled TINYINT(1) NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE delivery_partners (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL UNIQUE,
    vehicle_number VARCHAR(20),
    supplier_id BIGINT UNSIGNED,
    active      TINYINT(1) NOT NULL DEFAULT 1,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE delivery_addresses (
    id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED NOT NULL,
    label      VARCHAR(50),
    address    VARCHAR(255) NOT NULL,
    city       VARCHAR(80),
    state      VARCHAR(80),
    pincode    VARCHAR(10),
    latitude   DECIMAL(9,6),
    longitude  DECIMAL(9,6),
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============ CATALOG ============
CREATE TABLE categories (
    id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name       VARCHAR(120) NOT NULL,
    parent_id  BIGINT UNSIGNED NULL,
    image_url  VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES categories(id),
    INDEX idx_cat_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE products (
    id           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    uuid         CHAR(36) NOT NULL UNIQUE,
    supplier_id  BIGINT UNSIGNED NOT NULL,
    category_id  BIGINT UNSIGNED,
    name         VARCHAR(180) NOT NULL,
    brand        VARCHAR(120),
    barcode      VARCHAR(64),
    sku          VARCHAR(64),
    unit         VARCHAR(20) NOT NULL DEFAULT 'pcs',
    pack_size    VARCHAR(40),
    mrp          DECIMAL(12,2) NOT NULL,
    selling_price DECIMAL(12,2) NOT NULL,
    gst_rate     DECIMAL(5,2) NOT NULL DEFAULT 0,
    hsn_code     VARCHAR(12),
    image_url    VARCHAR(255),
    active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP NULL,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    FOREIGN KEY (category_id) REFERENCES categories(id),
    INDEX idx_prod_supplier (supplier_id),
    INDEX idx_prod_barcode (barcode),
    INDEX idx_prod_name (name),
    FULLTEXT idx_prod_search (name, brand)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE favourite_products (
    retailer_id BIGINT UNSIGNED NOT NULL,
    product_id  BIGINT UNSIGNED NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (retailer_id, product_id),
    FOREIGN KEY (retailer_id) REFERENCES retailers(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============ INVENTORY ============
CREATE TABLE inventory_items (
    id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    retailer_id   BIGINT UNSIGNED NOT NULL,
    product_id    BIGINT UNSIGNED NOT NULL,
    quantity      DECIMAL(12,3) NOT NULL DEFAULT 0,
    reorder_level DECIMAL(12,3) NOT NULL DEFAULT 0,
    cost_price    DECIMAL(12,2),
    version       BIGINT NOT NULL DEFAULT 0,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_inv_retailer_product (retailer_id, product_id),
    FOREIGN KEY (retailer_id) REFERENCES retailers(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_inv_qty_nonneg CHECK (quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inventory_batches (
    id               BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    inventory_item_id BIGINT UNSIGNED NOT NULL,
    batch_number     VARCHAR(64),
    quantity         DECIMAL(12,3) NOT NULL DEFAULT 0,
    expiry_date      DATE,
    cost_price       DECIMAL(12,2),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id) ON DELETE CASCADE,
    INDEX idx_batch_expiry (expiry_date),
    CONSTRAINT chk_batch_qty_nonneg CHECK (quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock_movements (
    id               BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    inventory_item_id BIGINT UNSIGNED NOT NULL,
    batch_id         BIGINT UNSIGNED NULL,
    movement_type    ENUM('PURCHASE','SALE','RETURN','ADJUSTMENT','EXPIRY') NOT NULL,
    quantity_delta   DECIMAL(12,3) NOT NULL,
    reference_type   VARCHAR(40),
    reference_id     BIGINT UNSIGNED,
    note             VARCHAR(255),
    created_by       BIGINT UNSIGNED,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id) ON DELETE CASCADE,
    FOREIGN KEY (batch_id) REFERENCES inventory_batches(id),
    INDEX idx_move_item (inventory_item_id),
    INDEX idx_move_type (movement_type),
    INDEX idx_move_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============ CART & ORDERS ============
CREATE TABLE carts (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    retailer_id BIGINT UNSIGNED NOT NULL,
    supplier_id BIGINT UNSIGNED NOT NULL,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_cart_retailer_supplier (retailer_id, supplier_id),
    FOREIGN KEY (retailer_id) REFERENCES retailers(id) ON DELETE CASCADE,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cart_items (
    id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    cart_id    BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    quantity   DECIMAL(12,3) NOT NULL,
    UNIQUE KEY uq_cart_product (cart_id, product_id),
    FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE orders (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    uuid            CHAR(36) NOT NULL UNIQUE,
    order_number    VARCHAR(30) NOT NULL UNIQUE,
    retailer_id     BIGINT UNSIGNED NOT NULL,
    supplier_id     BIGINT UNSIGNED NOT NULL,
    status          ENUM('DRAFT','PENDING','ACCEPTED','PACKED','OUT_FOR_DELIVERY',
                         'DELIVERED','CASH_COLLECTED','COMPLETED','REJECTED','CANCELLED','RETURNED') NOT NULL DEFAULT 'DRAFT',
    order_source    ENUM('CART','REPEAT','VOICE','TEXT','IMAGE','BARCODE','AI') NOT NULL DEFAULT 'CART',
    payment_mode    ENUM('UPI','CARD','NET_BANKING','WALLET','COD','CREDIT','ADVANCE') NOT NULL,
    payment_status  ENUM('PENDING','PAID','PARTIAL','REFUNDED','FAILED') NOT NULL DEFAULT 'PENDING',
    subtotal        DECIMAL(12,2) NOT NULL DEFAULT 0,
    tax_amount      DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount    DECIMAL(12,2) NOT NULL DEFAULT 0,
    delivery_address_id BIGINT UNSIGNED,
    placed_at       TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (retailer_id) REFERENCES retailers(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    FOREIGN KEY (delivery_address_id) REFERENCES delivery_addresses(id),
    INDEX idx_order_retailer (retailer_id),
    INDEX idx_order_supplier (supplier_id),
    INDEX idx_order_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_items (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    order_id    BIGINT UNSIGNED NOT NULL,
    product_id  BIGINT UNSIGNED NOT NULL,
    product_name VARCHAR(180) NOT NULL,
    quantity    DECIMAL(12,3) NOT NULL,
    unit_price  DECIMAL(12,2) NOT NULL,
    gst_rate    DECIMAL(5,2) NOT NULL DEFAULT 0,
    line_total  DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_oitem_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_status_history (
    id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    order_id   BIGINT UNSIGNED NOT NULL,
    from_status VARCHAR(30),
    to_status   VARCHAR(30) NOT NULL,
    changed_by BIGINT UNSIGNED,
    note       VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_osh_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============ INVOICES ============
CREATE TABLE invoices (
    id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    invoice_number VARCHAR(30) NOT NULL UNIQUE,
    order_id      BIGINT UNSIGNED NOT NULL UNIQUE,
    supplier_id   BIGINT UNSIGNED NOT NULL,
    retailer_id   BIGINT UNSIGNED NOT NULL,
    subtotal      DECIMAL(12,2) NOT NULL,
    tax_amount    DECIMAL(12,2) NOT NULL,
    total_amount  DECIMAL(12,2) NOT NULL,
    pdf_url       VARCHAR(255),
    issued_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    FOREIGN KEY (retailer_id) REFERENCES retailers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invoice_items (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    invoice_id  BIGINT UNSIGNED NOT NULL,
    product_name VARCHAR(180) NOT NULL,
    quantity    DECIMAL(12,3) NOT NULL,
    unit_price  DECIMAL(12,2) NOT NULL,
    gst_rate    DECIMAL(5,2) NOT NULL,
    line_total  DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============ PAYMENTS, WALLET, CREDIT ============
CREATE TABLE payments (
    id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    uuid          CHAR(36) NOT NULL UNIQUE,
    order_id      BIGINT UNSIGNED NOT NULL,
    retailer_id   BIGINT UNSIGNED NOT NULL,
    mode          ENUM('UPI','CARD','NET_BANKING','WALLET','COD','CREDIT','ADVANCE') NOT NULL,
    amount        DECIMAL(12,2) NOT NULL,
    status        ENUM('INITIATED','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'INITIATED',
    gateway       VARCHAR(40),
    gateway_ref   VARCHAR(120),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (retailer_id) REFERENCES retailers(id),
    INDEX idx_pay_order (order_id),
    UNIQUE KEY uq_pay_gateway_ref (gateway_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wallets (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    retailer_id BIGINT UNSIGNED NOT NULL UNIQUE,
    balance     DECIMAL(12,2) NOT NULL DEFAULT 0,
    version     BIGINT NOT NULL DEFAULT 0,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (retailer_id) REFERENCES retailers(id) ON DELETE CASCADE,
    CONSTRAINT chk_wallet_nonneg CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE credit_accounts (
    id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    retailer_id   BIGINT UNSIGNED NOT NULL,
    supplier_id   BIGINT UNSIGNED NOT NULL,
    credit_limit  DECIMAL(12,2) NOT NULL DEFAULT 0,
    outstanding   DECIMAL(12,2) NOT NULL DEFAULT 0,
    approved_by   BIGINT UNSIGNED,
    status        ENUM('ACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    UNIQUE KEY uq_credit_retailer_supplier (retailer_id, supplier_id),
    FOREIGN KEY (retailer_id) REFERENCES retailers(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE transactions (
    id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    uuid          CHAR(36) NOT NULL UNIQUE,
    account_type  ENUM('WALLET','CREDIT','PAYMENT') NOT NULL,
    account_id    BIGINT UNSIGNED NOT NULL,
    payment_id    BIGINT UNSIGNED NULL,
    direction     ENUM('CREDIT','DEBIT') NOT NULL,
    amount        DECIMAL(12,2) NOT NULL,
    balance_after DECIMAL(12,2),
    description   VARCHAR(255),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payments(id),
    INDEX idx_txn_account (account_type, account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============ DELIVERY ============
CREATE TABLE deliveries (
    id                 BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    order_id           BIGINT UNSIGNED NOT NULL UNIQUE,
    delivery_partner_id BIGINT UNSIGNED,
    status             ENUM('ASSIGNED','PICKED_UP','OUT_FOR_DELIVERY','DELIVERED','FAILED') NOT NULL DEFAULT 'ASSIGNED',
    otp_hash           VARCHAR(255),
    cod_amount         DECIMAL(12,2) DEFAULT 0,
    cod_collected      TINYINT(1) NOT NULL DEFAULT 0,
    assigned_at        TIMESTAMP NULL,
    delivered_at       TIMESTAMP NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (delivery_partner_id) REFERENCES delivery_partners(id),
    INDEX idx_del_partner (delivery_partner_id),
    INDEX idx_del_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE proof_of_delivery (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    delivery_id BIGINT UNSIGNED NOT NULL UNIQUE,
    photo_url   VARCHAR(255),
    signature_url VARCHAR(255),
    latitude    DECIMAL(9,6),
    longitude   DECIMAL(9,6),
    verified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============ OFFERS, NOTIFICATIONS, AUDIT ============
CREATE TABLE offers (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT UNSIGNED NOT NULL,
    product_id  BIGINT UNSIGNED,
    title       VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    discount_type ENUM('PERCENT','FLAT') NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    min_quantity DECIMAL(12,3),
    starts_at   TIMESTAMP NULL,
    ends_at     TIMESTAMP NULL,
    active      TINYINT(1) NOT NULL DEFAULT 1,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notifications (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    type        ENUM('LOW_STOCK','OFFER','PAYMENT_REMINDER','DELIVERY_UPDATE','AI_RECOMMENDATION','ORDER_UPDATE','SYSTEM') NOT NULL,
    channel     ENUM('PUSH','IN_APP','EMAIL','WHATSAPP','SMS') NOT NULL DEFAULT 'IN_APP',
    title       VARCHAR(150) NOT NULL,
    body        VARCHAR(500),
    data_json   JSON,
    read_at     TIMESTAMP NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user (user_id),
    INDEX idx_notif_unread (user_id, read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_logs (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED,
    action      VARCHAR(80) NOT NULL,
    entity_type VARCHAR(60),
    entity_id   BIGINT UNSIGNED,
    ip_address  VARCHAR(45),
    details_json JSON,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============ PROCUREMENT (feature-flagged off in V1) ============
CREATE TABLE procurement_campaigns (
    id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    product_id    BIGINT UNSIGNED NOT NULL,
    target_quantity DECIMAL(12,3) NOT NULL,
    committed_quantity DECIMAL(12,3) NOT NULL DEFAULT 0,
    expected_price DECIMAL(12,2),
    closes_at     TIMESTAMP NULL,
    status        ENUM('OPEN','CLOSED','FULFILLED','CANCELLED') NOT NULL DEFAULT 'OPEN',
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE procurement_commitments (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    campaign_id BIGINT UNSIGNED NOT NULL,
    retailer_id BIGINT UNSIGNED NOT NULL,
    quantity    DECIMAL(12,3) NOT NULL,
    advance_paid DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (campaign_id) REFERENCES procurement_campaigns(id) ON DELETE CASCADE,
    FOREIGN KEY (retailer_id) REFERENCES retailers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============ SEED ROLES ============
INSERT INTO roles (name) VALUES ('RETAILER'),('SUPPLIER'),('DELIVERY_PARTNER'),('ADMIN');

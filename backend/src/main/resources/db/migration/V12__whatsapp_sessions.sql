-- Stateful WhatsApp conversation sessions: drive the interactive menu,
-- draft-order pipeline, variant disambiguation and confirmation per (distributor, phone).
CREATE TABLE whatsapp_sessions (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT UNSIGNED NOT NULL,
    phone       VARCHAR(20) NOT NULL,
    step        VARCHAR(40) NOT NULL,
    data_json   TEXT,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_wa_session (supplier_id, phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

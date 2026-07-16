CREATE TABLE external_identities (
    id               BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id          BIGINT UNSIGNED NOT NULL,
    provider         VARCHAR(20) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    email            VARCHAR(150),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at    TIMESTAMP NULL,
    CONSTRAINT uk_external_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT fk_external_identity_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_external_identity_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE oauth_login_codes (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    code_hash   CHAR(64) NOT NULL UNIQUE,
    user_id     BIGINT UNSIGNED NOT NULL,
    provider    VARCHAR(20) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_oauth_login_code_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_oauth_code_expiry (expires_at, consumed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
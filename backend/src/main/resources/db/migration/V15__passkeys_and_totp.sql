ALTER TABLE refresh_tokens
    ADD COLUMN authenticated_at TIMESTAMP NULL,
    ADD COLUMN auth_method VARCHAR(20) NULL;

CREATE TABLE passkey_credentials (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    credential_id VARBINARY(1024) NOT NULL,
    user_handle VARBINARY(64) NOT NULL,
    public_key_cose BLOB NOT NULL,
    signature_count BIGINT NOT NULL DEFAULT 0,
    display_name VARCHAR(100) NOT NULL,
    transports VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP NULL,
    UNIQUE KEY uk_passkey_credential_id (credential_id),
    INDEX idx_passkey_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE totp_credentials (
    user_id BIGINT UNSIGNED PRIMARY KEY,
    encrypted_secret VARCHAR(512) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    last_used_step BIGINT,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE authentication_challenges (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    token_hash CHAR(64) NOT NULL UNIQUE,
    user_id BIGINT UNSIGNED NOT NULL,
    challenge_type ENUM('TOTP_LOGIN') NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_auth_challenge_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE webauthn_ceremonies (
    id CHAR(36) PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    ceremony_type ENUM('REGISTER','AUTHENTICATE') NOT NULL,
    request_json TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_webauthn_ceremony_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

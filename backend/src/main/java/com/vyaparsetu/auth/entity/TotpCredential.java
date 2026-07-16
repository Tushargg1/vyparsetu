package com.vyaparsetu.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "totp_credentials")
@Getter
@Setter
public class TotpCredential {
    @Id
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "encrypted_secret", nullable = false)
    private String encryptedSecret;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "last_used_step")
    private Long lastUsedStep;
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;
    @Column(name = "locked_until")
    private Instant lockedUntil;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "verified_at")
    private Instant verifiedAt;
}

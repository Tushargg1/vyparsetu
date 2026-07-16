package com.vyaparsetu.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "passkey_credentials")
@Getter
@Setter
public class PasskeyCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "credential_id", nullable = false, columnDefinition = "VARBINARY(1024)")
    private byte[] credentialId;
    @Column(name = "user_handle", nullable = false)
    private byte[] userHandle;
    @Lob
    @Column(name = "public_key_cose", nullable = false)
    private byte[] publicKeyCose;
    @Column(name = "signature_count", nullable = false)
    private long signatureCount;
    @Column(name = "display_name", nullable = false)
    private String displayName;
    private String transports;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}

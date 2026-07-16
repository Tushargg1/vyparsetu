package com.vyaparsetu.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "webauthn_ceremonies")
@Getter
@Setter
public class WebAuthnCeremony {
    public enum Type { REGISTER, AUTHENTICATE }
    @Id
    private String id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(name = "ceremony_type", nullable = false)
    private Type ceremonyType;
    @Lob
    @Column(name = "request_json", nullable = false)
    private String requestJson;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "consumed_at")
    private Instant consumedAt;
}

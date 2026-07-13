package com.vyaparsetu.whatsapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** A stored WhatsApp message (inbound or outbound) for history + idempotency. */
@Entity
@Table(name = "whatsapp_messages")
@Getter
@Setter
public class WhatsAppMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String direction; // IN | OUT

    @Column(length = 4000)
    private String body;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}

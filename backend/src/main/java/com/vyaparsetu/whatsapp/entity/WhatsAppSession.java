package com.vyaparsetu.whatsapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Persisted conversation state for a (distributor, phone) WhatsApp chat. */
@Entity
@Table(name = "whatsapp_sessions")
@Getter
@Setter
public class WhatsAppSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String step = "NONE";

    @Column(name = "data_json", length = 10000)
    private String dataJson;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
}

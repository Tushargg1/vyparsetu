package com.vyaparsetu.whatsapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** A draft order awaiting "YES" confirmation from a WhatsApp customer. */
@Entity
@Table(name = "whatsapp_pending_orders")
@Getter
@Setter
public class WhatsAppPendingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "retailer_id", nullable = false)
    private Long retailerId;

    @Column(nullable = false)
    private String phone;

    @Column(name = "raw_text", nullable = false)
    private String rawText;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}

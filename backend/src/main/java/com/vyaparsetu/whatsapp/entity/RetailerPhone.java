package com.vyaparsetu.whatsapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** An additional WhatsApp number linked to a retailer account. */
@Entity
@Table(name = "retailer_phones")
@Getter
@Setter
public class RetailerPhone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "retailer_id", nullable = false)
    private Long retailerId;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}

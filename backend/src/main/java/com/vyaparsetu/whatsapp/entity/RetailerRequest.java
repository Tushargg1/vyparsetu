package com.vyaparsetu.whatsapp.entity;

import com.vyaparsetu.whatsapp.WhatsAppEnums;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** A new-customer onboarding request awaiting seller approval. */
@Entity
@Table(name = "retailer_requests")
@Getter
@Setter
public class RetailerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "shop_name")
    private String shopName;

    @Column(name = "gst_number")
    private String gstNumber;

    private String address;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WhatsAppEnums.RequestStatus status = WhatsAppEnums.RequestStatus.PENDING;

    private String message;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}

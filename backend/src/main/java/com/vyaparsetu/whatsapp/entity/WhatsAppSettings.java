package com.vyaparsetu.whatsapp.entity;

import com.vyaparsetu.whatsapp.WhatsAppEnums;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "whatsapp_settings")
@Getter
@Setter
public class WhatsAppSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false, unique = true)
    private Long supplierId;

    @Column(nullable = false)
    private boolean connected = false;

    @Column(name = "business_number")
    private String businessNumber;

    @Column(name = "ai_enabled", nullable = false)
    private boolean aiEnabled = true;

    @Column(name = "auto_reply", nullable = false)
    private boolean autoReply = true;

    @Column(name = "auto_create_orders", nullable = false)
    private boolean autoCreateOrders = false;

    @Column(name = "require_confirmation", nullable = false)
    private boolean requireConfirmation = true;

    @Column(name = "human_takeover", nullable = false)
    private boolean humanTakeover = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "seller_approval_mode", nullable = false)
    private WhatsAppEnums.SellerApprovalMode sellerApprovalMode = WhatsAppEnums.SellerApprovalMode.FIRST_ORDER_ONLY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WhatsAppEnums.ChatLanguage language = WhatsAppEnums.ChatLanguage.BOTH;

    @Column(name = "business_hours_start", nullable = false)
    private String businessHoursStart = "09:00";

    @Column(name = "business_hours_end", nullable = false)
    private String businessHoursEnd = "20:00";

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
}

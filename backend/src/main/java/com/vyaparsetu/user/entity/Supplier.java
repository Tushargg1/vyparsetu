package com.vyaparsetu.user.entity;

import com.vyaparsetu.common.enums.Enums;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_type", nullable = false)
    private Enums.SupplierType supplierType;

    @Column(name = "gst_number")
    private String gstNumber;

    private String address;
    private String city;
    private String state;
    private String pincode;

    /** Extra contact numbers beyond the primary login phone (comma-separated). */
    @Column(name = "alt_phones", length = 512)
    private String altPhones;

    /** A shared location link (e.g. a Google Maps pin) or "lat,lng". */
    @Column(name = "location_url", length = 1024)
    private String locationUrl;

    @Column(name = "whatsapp_enabled", nullable = false)
    private boolean whatsappEnabled = false;

    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    @Column(name = "invite_code", unique = true)
    private String inviteCode;
}

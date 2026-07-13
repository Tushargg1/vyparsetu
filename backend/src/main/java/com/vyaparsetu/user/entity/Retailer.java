package com.vyaparsetu.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "retailers")
@Getter
@Setter
public class Retailer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "shop_name", nullable = false)
    private String shopName;

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

    @Column(name = "credit_approved", nullable = false)
    private boolean creditApproved = false;

    /** The single distributor (supplier) this retailer belongs to. */
    @Column(name = "distributor_id")
    private Long distributorId;
}

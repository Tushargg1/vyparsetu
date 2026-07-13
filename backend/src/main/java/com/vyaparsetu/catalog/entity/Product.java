package com.vyaparsetu.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false)
    private String name;

    private String brand;
    private String barcode;
    private String sku;

    @Column(nullable = false)
    private String unit = "pcs";

    @Column(name = "pack_size")
    private String packSize;

    @Column(nullable = false)
    private BigDecimal mrp;

    @Column(name = "selling_price", nullable = false)
    private BigDecimal sellingPrice;

    @Column(name = "gst_rate", nullable = false)
    private BigDecimal gstRate = BigDecimal.ZERO;

    @Column(name = "stock_qty", nullable = false)
    private BigDecimal stockQty = BigDecimal.ZERO;

    @Column(name = "reserved_qty", nullable = false)
    private BigDecimal reservedQty = BigDecimal.ZERO;

    @Column(name = "low_stock_threshold", nullable = false)
    private BigDecimal lowStockThreshold = BigDecimal.ZERO;

    @Column(name = "track_stock", nullable = false)
    private boolean trackStock = false;

    @Column(name = "hsn_code")
    private String hsnCode;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Sellable quantity = on-hand minus reserved (never negative). */
    @Transient
    public BigDecimal getAvailableQty() {
        BigDecimal a = (stockQty == null ? BigDecimal.ZERO : stockQty)
                .subtract(reservedQty == null ? BigDecimal.ZERO : reservedQty);
        return a.signum() < 0 ? BigDecimal.ZERO : a;
    }
}

package com.vyaparsetu.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "carts")
@Getter
@Setter
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "retailer_id", nullable = false)
    private Long retailerId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
}

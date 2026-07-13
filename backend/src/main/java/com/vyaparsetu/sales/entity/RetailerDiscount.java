package com.vyaparsetu.sales.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "retailer_discounts")
@Getter
@Setter
public class RetailerDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "retailer_id", nullable = false)
    private Long retailerId;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private BigDecimal percent;
}

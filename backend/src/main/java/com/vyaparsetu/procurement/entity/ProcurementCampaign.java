package com.vyaparsetu.procurement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "procurement_campaigns")
@Getter
@Setter
public class ProcurementCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "target_quantity", nullable = false)
    private BigDecimal targetQuantity;

    @Column(name = "committed_quantity", nullable = false)
    private BigDecimal committedQuantity = BigDecimal.ZERO;

    @Column(name = "expected_price")
    private BigDecimal expectedPrice;

    @Column(name = "closes_at")
    private Instant closesAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public enum Status { OPEN, CLOSED, FULFILLED, CANCELLED }
}

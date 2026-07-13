package com.vyaparsetu.procurement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "procurement_commitments")
@Getter
@Setter
public class ProcurementCommitment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "retailer_id", nullable = false)
    private Long retailerId;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "advance_paid", nullable = false)
    private BigDecimal advancePaid = BigDecimal.ZERO;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}

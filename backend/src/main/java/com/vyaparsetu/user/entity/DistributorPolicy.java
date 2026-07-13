package com.vyaparsetu.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Configurable ordering policies for a distributor (supplier). */
@Entity
@Table(name = "distributor_policies")
@Getter
@Setter
public class DistributorPolicy {

    public enum OutOfStockBehavior { REJECT, BACKORDER, PARTIAL }

    public enum CreditOverLimitAction { BLOCK, REQUIRE_APPROVAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false, unique = true)
    private Long supplierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "out_of_stock_behavior", nullable = false)
    private OutOfStockBehavior outOfStockBehavior = OutOfStockBehavior.REJECT;

    @Column(name = "min_order_value", nullable = false)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "min_order_qty", nullable = false)
    private BigDecimal minOrderQty = BigDecimal.ZERO;

    @Column(name = "auto_cancel_hours", nullable = false)
    private int autoCancelHours = 0;

    @Column(name = "enforce_credit_limit", nullable = false)
    private boolean enforceCreditLimit = false;

    /** What to do when an order would push a retailer past their credit limit. */
    @Enumerated(EnumType.STRING)
    @Column(name = "credit_over_limit_action", nullable = false)
    private CreditOverLimitAction creditOverLimitAction = CreditOverLimitAction.BLOCK;

    /** Orders at/above this amount raise a LARGE_ORDER alert to the distributor (0 = disabled). */
    @Column(name = "large_order_threshold", nullable = false)
    private BigDecimal largeOrderThreshold = BigDecimal.ZERO;

    /** When false, new orders land in PENDING for distributor approval rather than auto-accepting. */
    @Column(name = "allow_ordering_without_approval", nullable = false)
    private boolean allowOrderingWithoutApproval = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
}

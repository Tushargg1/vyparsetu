package com.vyaparsetu.analytics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Append-only analytics event. Powers dashboards and future AI recommendations.
 * Kept intentionally generic so new metrics need no schema change.
 */
@Entity
@Table(name = "analytics_events")
@Getter
@Setter
public class AnalyticsEvent {

    public enum EventType {
        ORDER_PLACED,
        ORDER_MODIFIED,
        PRODUCT_ORDERED,
        AI_EXTRACTION_FAILED,
        VALIDATION_FAILURE,
        ALIAS_USED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "retailer_id")
    private Long retailerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "product_id")
    private Long productId;

    /** Optional numeric payload, e.g. order value, quantity. */
    @Column(name = "numeric_value")
    private BigDecimal numericValue;

    /** Optional text payload, e.g. raw text that failed extraction, alias used. */
    @Column(name = "text_value", length = 512)
    private String textValue;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}

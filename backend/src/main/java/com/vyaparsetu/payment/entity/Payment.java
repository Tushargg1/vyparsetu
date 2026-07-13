package com.vyaparsetu.payment.entity;

import com.vyaparsetu.common.enums.Enums;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "retailer_id", nullable = false)
    private Long retailerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.PaymentMode mode;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentTxnStatus status = PaymentTxnStatus.INITIATED;

    private String gateway;

    @Column(name = "gateway_ref")
    private String gatewayRef;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public enum PaymentTxnStatus { INITIATED, SUCCESS, FAILED, REFUNDED }
}

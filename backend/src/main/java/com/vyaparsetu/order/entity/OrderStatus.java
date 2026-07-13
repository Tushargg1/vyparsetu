package com.vyaparsetu.order.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    DRAFT,
    PENDING,
    ACCEPTED,
    PACKED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CASH_COLLECTED,
    COMPLETED,
    REJECTED,
    CANCELLED,
    RETURNED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            DRAFT, EnumSet.of(PENDING, CANCELLED),
            PENDING, EnumSet.of(ACCEPTED, REJECTED, CANCELLED),
            ACCEPTED, EnumSet.of(PACKED, CANCELLED),
            PACKED, EnumSet.of(OUT_FOR_DELIVERY),
            OUT_FOR_DELIVERY, EnumSet.of(DELIVERED),
            DELIVERED, EnumSet.of(CASH_COLLECTED, COMPLETED, RETURNED),
            CASH_COLLECTED, EnumSet.of(COMPLETED),
            COMPLETED, EnumSet.noneOf(OrderStatus.class),
            REJECTED, EnumSet.noneOf(OrderStatus.class),
            CANCELLED, EnumSet.noneOf(OrderStatus.class)
    );

    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, EnumSet.noneOf(OrderStatus.class)).contains(target);
    }
}

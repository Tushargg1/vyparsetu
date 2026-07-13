package com.vyaparsetu.order;

import com.vyaparsetu.order.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Property 4: only valid order state transitions are allowed. */
class OrderStatusTest {

    @Test
    void allowsValidTransitions() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.ACCEPTED));
        assertTrue(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.PACKED));
        assertTrue(OrderStatus.PACKED.canTransitionTo(OrderStatus.OUT_FOR_DELIVERY));
        assertTrue(OrderStatus.OUT_FOR_DELIVERY.canTransitionTo(OrderStatus.DELIVERED));
        assertTrue(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.COMPLETED));
    }

    @Test
    void rejectsInvalidTransitions() {
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.ACCEPTED));
        assertFalse(OrderStatus.REJECTED.canTransitionTo(OrderStatus.ACCEPTED));
    }
}

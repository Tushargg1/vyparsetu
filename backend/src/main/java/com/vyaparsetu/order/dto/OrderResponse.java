package com.vyaparsetu.order.dto;

import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.order.entity.Order;
import com.vyaparsetu.order.entity.OrderItem;
import com.vyaparsetu.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String uuid,
        String orderNumber,
        Long retailerId,
        Long supplierId,
        OrderStatus status,
        Enums.OrderSource orderSource,
        Enums.PaymentMode paymentMode,
        Enums.PaymentStatus paymentStatus,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        Instant lastPaymentAt,
        Instant placedAt,
        Instant acceptedAt,
        Instant packedAt,
        Instant deliveredAt,
        List<Item> items
) {
    public record Item(
            Long productId,
            String productName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal gstRate,
            BigDecimal lineTotal
    ) {
        public static Item from(OrderItem oi) {
            return new Item(oi.getProductId(), oi.getProductName(), oi.getQuantity(),
                    oi.getUnitPrice(), oi.getGstRate(), oi.getLineTotal());
        }
    }

    public static OrderResponse from(Order o, List<OrderItem> items) {
        return new OrderResponse(
                o.getId(), o.getUuid(), o.getOrderNumber(), o.getRetailerId(), o.getSupplierId(),
                o.getStatus(), o.getOrderSource(), o.getPaymentMode(), o.getPaymentStatus(),
                o.getSubtotal(), o.getTaxAmount(), o.getDiscountAmount(), o.getTotalAmount(),
                o.getAmountPaid(), o.getLastPaymentAt(),
                o.getPlacedAt(), o.getAcceptedAt(), o.getPackedAt(), o.getDeliveredAt(),
                items.stream().map(Item::from).toList());
    }
}

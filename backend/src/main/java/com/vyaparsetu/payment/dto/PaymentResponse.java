package com.vyaparsetu.payment.dto;

import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.payment.entity.Payment;

import java.math.BigDecimal;

public record PaymentResponse(
        String uuid,
        Long orderId,
        Enums.PaymentMode mode,
        BigDecimal amount,
        String status,
        String gatewayRef
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getUuid(), p.getOrderId(), p.getMode(),
                p.getAmount(), p.getStatus().name(), p.getGatewayRef());
    }
}

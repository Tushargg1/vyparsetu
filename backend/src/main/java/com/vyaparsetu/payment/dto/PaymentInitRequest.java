package com.vyaparsetu.payment.dto;

import com.vyaparsetu.common.enums.Enums;
import jakarta.validation.constraints.NotNull;

public record PaymentInitRequest(
        @NotNull Long orderId,
        @NotNull Enums.PaymentMode mode
) {
}

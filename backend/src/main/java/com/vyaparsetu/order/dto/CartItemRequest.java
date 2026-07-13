package com.vyaparsetu.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CartItemRequest(
        @NotNull Long supplierId,
        @NotNull Long productId,
        @NotNull @Positive BigDecimal quantity
) {
}

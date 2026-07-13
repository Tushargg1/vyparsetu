package com.vyaparsetu.inventory.dto;

import com.vyaparsetu.common.enums.Enums;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A stock change request. quantity is always positive; the movementType
 * determines whether it increases (PURCHASE/RETURN) or decreases (SALE/EXPIRY) stock.
 * ADJUSTMENT uses the signed delta directly.
 */
public record StockMovementRequest(
        @NotNull Long productId,
        @NotNull Enums.MovementType movementType,
        @NotNull @Positive BigDecimal quantity,
        BigDecimal costPrice,
        String batchNumber,
        LocalDate expiryDate,
        String note
) {
}

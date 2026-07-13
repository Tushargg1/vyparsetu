package com.vyaparsetu.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        String brand,
        Long categoryId,
        String barcode,
        String sku,
        String unit,
        String packSize,
        @NotNull @PositiveOrZero BigDecimal mrp,
        @NotNull @PositiveOrZero BigDecimal sellingPrice,
        @PositiveOrZero BigDecimal gstRate,
        String hsnCode,
        String imageUrl,
        @PositiveOrZero BigDecimal stockQty,
        @PositiveOrZero BigDecimal lowStockThreshold,
        Boolean trackStock
) {
}

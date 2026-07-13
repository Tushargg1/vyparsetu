package com.vyaparsetu.sales.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class SalesDtos {

    private SalesDtos() {
    }

    public record SaleLine(@NotNull Long productId, @NotNull @Positive BigDecimal quantity) {
    }

    public record RecordSaleRequest(@NotEmpty List<SaleLine> items) {
    }

    public record SaleItemResponse(Long productId, String productName, BigDecimal quantity,
                                   BigDecimal unitPrice, BigDecimal costPrice, BigDecimal lineTotal) {
    }

    public record SaleResponse(Long id, BigDecimal totalAmount, BigDecimal totalItems,
                               Instant createdAt, List<SaleItemResponse> items) {
    }

    public record LookupResponse(Long productId, String productName, String brand,
                                 BigDecimal price, BigDecimal inStock) {
    }

    public record RateListItem(Long productId, String productName, String brand,
                               BigDecimal mrp, BigDecimal distributorPrice, BigDecimal myPrice, BigDecimal inStock) {
    }

    public record SetRateRequest(@NotNull Long productId, @NotNull @Positive BigDecimal price) {
    }

    public record DiscountResponse(Long id, String label, BigDecimal percent) {
    }

    public record DiscountRequest(@jakarta.validation.constraints.NotBlank String label,
                                  @NotNull @Positive BigDecimal percent) {
    }
}

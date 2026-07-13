package com.vyaparsetu.report.dto;

import java.math.BigDecimal;
import java.time.Instant;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record SalesReport(Instant from, Instant to, BigDecimal salesValue,
                              BigDecimal cogs, BigDecimal profit) {
    }

    public record Bucket(String label, BigDecimal sales, BigDecimal purchases) {
    }

    public record RevenueResponse(
            BigDecimal totalSales,
            BigDecimal totalPurchases,
            BigDecimal profit,
            BigDecimal stockValue,
            java.util.List<Bucket> monthly,
            java.util.List<Bucket> weekly,
            java.util.List<Bucket> yearly
    ) {
    }

    public record TopProduct(Long productId, String productName, BigDecimal unitsSold) {
    }

    public record DaySummary(BigDecimal sales, BigDecimal profit) {
    }

    public record PurchaseReport(Instant from, Instant to, BigDecimal purchaseValue, long orderCount) {
    }

    public record InventoryReport(BigDecimal inventoryValueAtCost) {
    }

    public record SupplierSalesReport(Instant from, Instant to, BigDecimal salesValue, long orderCount) {
    }
}

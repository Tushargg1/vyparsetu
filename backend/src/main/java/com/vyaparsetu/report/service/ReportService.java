package com.vyaparsetu.report.service;

import com.vyaparsetu.report.dto.ReportDtos;
import com.vyaparsetu.report.repository.ReportRepository;
import com.vyaparsetu.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserService userService;

    public ReportService(ReportRepository reportRepository, UserService userService) {
        this.reportRepository = reportRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public ReportDtos.SalesReport retailerSales(Instant from, Instant to) {
        Long rid = userService.currentRetailerId();
        BigDecimal sales = nz(reportRepository.retailerSalesValue(rid, from, to));
        BigDecimal cogs = nz(reportRepository.retailerCogs(rid, from, to));
        return new ReportDtos.SalesReport(from, to, sales, cogs, sales.subtract(cogs));
    }

    @Transactional(readOnly = true)
    public ReportDtos.PurchaseReport retailerPurchases(Instant from, Instant to) {
        Long rid = userService.currentRetailerId();
        return new ReportDtos.PurchaseReport(from, to,
                nz(reportRepository.retailerPurchaseValue(rid, from, to)),
                reportRepository.retailerOrderCount(rid, from, to));
    }

    @Transactional(readOnly = true)
    public ReportDtos.InventoryReport retailerInventory() {
        Long rid = userService.currentRetailerId();
        return new ReportDtos.InventoryReport(nz(reportRepository.inventoryValue(rid)));
    }

    @Transactional(readOnly = true)
    public ReportDtos.SupplierSalesReport supplierSales(Instant from, Instant to) {
        Long sid = userService.currentSupplierId();
        return new ReportDtos.SupplierSalesReport(from, to,
                nz(reportRepository.supplierSalesValue(sid, from, to)),
                reportRepository.supplierOrderCount(sid, from, to));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // ---------------- Revenue dashboard (role-aware) ----------------

    private boolean isSupplier() {
        return com.vyaparsetu.common.security.CurrentUser.get().roles().contains("SUPPLIER");
    }

    @Transactional(readOnly = true)
    public ReportDtos.RevenueResponse revenue() {
        Instant now = Instant.now();
        Instant epoch = Instant.parse("2000-01-01T00:00:00Z");
        if (isSupplier()) {
            Long sid = userService.currentSupplierId();
            BigDecimal totalSales = nz(reportRepository.supplierSalesValue(sid, epoch, now));
            return new ReportDtos.RevenueResponse(totalSales, BigDecimal.ZERO, null, null,
                    supplierBuckets(sid, java.time.temporal.ChronoUnit.MONTHS, 12),
                    supplierBuckets(sid, java.time.temporal.ChronoUnit.WEEKS, 8),
                    supplierBuckets(sid, java.time.temporal.ChronoUnit.YEARS, 5));
        }
        Long rid = userService.currentRetailerId();
        BigDecimal totalSales = nz(reportRepository.retailerSalesValue(rid, epoch, now));
        BigDecimal cogs = nz(reportRepository.retailerCogs(rid, epoch, now));
        BigDecimal purchases = nz(reportRepository.retailerPurchaseValue(rid, epoch, now));
        BigDecimal stockValue = nz(reportRepository.inventoryValue(rid));
        return new ReportDtos.RevenueResponse(totalSales, purchases, totalSales.subtract(cogs), stockValue,
                retailerBuckets(rid, java.time.temporal.ChronoUnit.MONTHS, 12),
                retailerBuckets(rid, java.time.temporal.ChronoUnit.WEEKS, 8),
                retailerBuckets(rid, java.time.temporal.ChronoUnit.YEARS, 5));
    }

    private java.util.List<ReportDtos.Bucket> retailerBuckets(Long rid, java.time.temporal.ChronoUnit unit, int count) {
        java.util.List<ReportDtos.Bucket> out = new java.util.ArrayList<>();
        for (int i = count - 1; i >= 0; i--) {
            Instant[] range = bucketRange(unit, i);
            BigDecimal sales = nz(reportRepository.retailerSalesValue(rid, range[0], range[1]));
            BigDecimal purchases = nz(reportRepository.retailerPurchaseValue(rid, range[0], range[1]));
            out.add(new ReportDtos.Bucket(bucketLabel(unit, i), sales, purchases));
        }
        return out;
    }

    private java.util.List<ReportDtos.Bucket> supplierBuckets(Long sid, java.time.temporal.ChronoUnit unit, int count) {
        java.util.List<ReportDtos.Bucket> out = new java.util.ArrayList<>();
        for (int i = count - 1; i >= 0; i--) {
            Instant[] range = bucketRange(unit, i);
            BigDecimal sales = nz(reportRepository.supplierSalesValue(sid, range[0], range[1]));
            out.add(new ReportDtos.Bucket(bucketLabel(unit, i), sales, BigDecimal.ZERO));
        }
        return out;
    }

    private Instant[] bucketRange(java.time.temporal.ChronoUnit unit, int agoIndex) {
        java.time.ZoneId z = java.time.ZoneOffset.UTC;
        java.time.LocalDate today = java.time.LocalDate.now(z);
        java.time.LocalDate start;
        java.time.LocalDate end;
        switch (unit) {
            case WEEKS -> {
                java.time.LocalDate weekStart = today.minusWeeks(agoIndex).with(java.time.DayOfWeek.MONDAY);
                start = weekStart;
                end = weekStart.plusWeeks(1);
            }
            case YEARS -> {
                int year = today.getYear() - agoIndex;
                start = java.time.LocalDate.of(year, 1, 1);
                end = start.plusYears(1);
            }
            default -> {
                java.time.LocalDate m = today.minusMonths(agoIndex).withDayOfMonth(1);
                start = m;
                end = m.plusMonths(1);
            }
        }
        return new Instant[]{ start.atStartOfDay(z).toInstant(), end.atStartOfDay(z).toInstant() };
    }

    private String bucketLabel(java.time.temporal.ChronoUnit unit, int agoIndex) {
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        return switch (unit) {
            case WEEKS -> "W" + today.minusWeeks(agoIndex).get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
            case YEARS -> String.valueOf(today.getYear() - agoIndex);
            default -> today.minusMonths(agoIndex).getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH);
        };
    }

    @Transactional(readOnly = true)
    public ReportDtos.DaySummary today() {
        java.time.Instant start = java.time.LocalDate.now(java.time.ZoneOffset.UTC)
                .atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        Instant now = Instant.now();
        if (isSupplier()) {
            BigDecimal sales = nz(reportRepository.supplierSalesValue(userService.currentSupplierId(), start, now));
            return new ReportDtos.DaySummary(sales, null);
        }
        Long rid = userService.currentRetailerId();
        BigDecimal sales = nz(reportRepository.retailerSalesValue(rid, start, now));
        BigDecimal cogs = nz(reportRepository.retailerCogs(rid, start, now));
        return new ReportDtos.DaySummary(sales, sales.subtract(cogs));
    }

    @Transactional(readOnly = true)
    public ReportDtos.RevenueResponse revenueRange(java.time.LocalDate from, java.time.LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new com.vyaparsetu.common.exception.BusinessException("BAD_RANGE",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid date range");
        }
        // cap to 92 days to keep it light
        if (java.time.temporal.ChronoUnit.DAYS.between(from, to) > 92) {
            from = to.minusDays(92);
        }
        java.time.ZoneId z = java.time.ZoneOffset.UTC;
        boolean supplier = isSupplier();
        Long id = supplier ? userService.currentSupplierId() : userService.currentRetailerId();

        java.util.List<ReportDtos.Bucket> daily = new java.util.ArrayList<>();
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalPurchases = BigDecimal.ZERO;
        for (java.time.LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            Instant start = day.atStartOfDay(z).toInstant();
            Instant end = day.plusDays(1).atStartOfDay(z).toInstant();
            BigDecimal sales = supplier
                    ? nz(reportRepository.supplierSalesValue(id, start, end))
                    : nz(reportRepository.retailerSalesValue(id, start, end));
            BigDecimal purchases = supplier ? BigDecimal.ZERO
                    : nz(reportRepository.retailerPurchaseValue(id, start, end));
            String label = day.getDayOfMonth() + " " + day.getMonth()
                    .getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH);
            daily.add(new ReportDtos.Bucket(label, sales, purchases));
            totalSales = totalSales.add(sales);
            totalPurchases = totalPurchases.add(purchases);
        }
        BigDecimal profit = supplier ? null : null;
        if (!supplier) {
            Instant s = from.atStartOfDay(z).toInstant();
            Instant e = to.plusDays(1).atStartOfDay(z).toInstant();
            profit = totalSales.subtract(nz(reportRepository.retailerCogs(id, s, e)));
        }
        return new ReportDtos.RevenueResponse(totalSales, totalPurchases, profit, null, daily, daily, daily);
    }

    @Transactional(readOnly = true)
    public java.util.List<ReportDtos.TopProduct> topProducts() {
        var rows = isSupplier()
                ? reportRepository.supplierTopProducts(userService.currentSupplierId())
                : reportRepository.retailerTopProducts(userService.currentRetailerId());
        return rows.stream()
                .map(r -> new ReportDtos.TopProduct(r.getProductId(), r.getProductName(), nz(r.getSold())))
                .toList();
    }
}

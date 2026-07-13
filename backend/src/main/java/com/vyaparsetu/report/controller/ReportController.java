package com.vyaparsetu.report.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.report.dto.ReportDtos;
import com.vyaparsetu.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Sales, purchase, inventory and profit reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales")
    @PreAuthorize("hasRole('RETAILER')")
    @Operation(summary = "Retailer sales and profit for a period (defaults to last 30 days)")
    public ApiResponse<ReportDtos.SalesReport> sales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant[] range = range(from, to);
        return ApiResponse.ok(reportService.retailerSales(range[0], range[1]));
    }

    @GetMapping("/purchase")
    @PreAuthorize("hasRole('RETAILER')")
    public ApiResponse<ReportDtos.PurchaseReport> purchases(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant[] range = range(from, to);
        return ApiResponse.ok(reportService.retailerPurchases(range[0], range[1]));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasRole('RETAILER')")
    public ApiResponse<ReportDtos.InventoryReport> inventory() {
        return ApiResponse.ok(reportService.retailerInventory());
    }

    @GetMapping("/supplier/sales")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ApiResponse<ReportDtos.SupplierSalesReport> supplierSales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant[] range = range(from, to);
        return ApiResponse.ok(reportService.supplierSales(range[0], range[1]));
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('RETAILER','SUPPLIER')")
    @Operation(summary = "Revenue dashboard (totals + month/week/year buckets) for the current role")
    public ApiResponse<ReportDtos.RevenueResponse> revenue() {
        return ApiResponse.ok(reportService.revenue());
    }

    @GetMapping("/revenue/range")
    @PreAuthorize("hasAnyRole('RETAILER','SUPPLIER')")
    @Operation(summary = "Revenue for a custom date range (daily buckets)")
    public ApiResponse<ReportDtos.RevenueResponse> revenueRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
        return ApiResponse.ok(reportService.revenueRange(from, to));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('RETAILER','SUPPLIER')")
    @Operation(summary = "Today's sales and profit for the current role")
    public ApiResponse<ReportDtos.DaySummary> today() {
        return ApiResponse.ok(reportService.today());
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasAnyRole('RETAILER','SUPPLIER')")
    @Operation(summary = "Best-selling products for the current role")
    public ApiResponse<java.util.List<ReportDtos.TopProduct>> topProducts() {
        return ApiResponse.ok(reportService.topProducts());
    }

    private Instant[] range(Instant from, Instant to) {
        Instant t = to != null ? to : Instant.now();
        Instant f = from != null ? from : t.minus(30, ChronoUnit.DAYS);
        return new Instant[]{f, t};
    }
}

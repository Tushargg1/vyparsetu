package com.vyaparsetu.sales.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.sales.dto.SalesDtos;
import com.vyaparsetu.sales.service.SalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sales")
@Tag(name = "Sales", description = "Retailer counter sales (scan & sell) and rate list")
@PreAuthorize("hasRole('RETAILER')")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping("/lookup")
    @Operation(summary = "Look up a product by barcode with my retail price and stock")
    public ApiResponse<SalesDtos.LookupResponse> lookup(@RequestParam String barcode) {
        return ApiResponse.ok(salesService.lookup(barcode));
    }

    @PostMapping
    @Operation(summary = "Record a counter sale (decrements stock, saves bill with date/time/total)")
    public ApiResponse<SalesDtos.SaleResponse> record(@Valid @RequestBody SalesDtos.RecordSaleRequest req) {
        return ApiResponse.ok(salesService.recordSale(req));
    }

    @GetMapping
    @Operation(summary = "Past counter sales")
    public ApiResponse<List<SalesDtos.SaleResponse>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.ok(salesService.history(PageRequest.of(page, size)));
    }

    @GetMapping("/rate-list")
    @Operation(summary = "My products with distributor price and my retail price")
    public ApiResponse<List<SalesDtos.RateListItem>> rateList() {
        return ApiResponse.ok(salesService.rateList());
    }

    @PutMapping("/rate-list")
    @Operation(summary = "Set my retail price for a product")
    public ApiResponse<Void> setRate(@Valid @RequestBody SalesDtos.SetRateRequest req) {
        salesService.setRate(req);
        return ApiResponse.ok(null);
    }

    @GetMapping("/discounts")
    @Operation(summary = "My reusable discount presets")
    public ApiResponse<List<SalesDtos.DiscountResponse>> discounts() {
        return ApiResponse.ok(salesService.discounts());
    }

    @PostMapping("/discounts")
    @Operation(summary = "Create a discount preset")
    public ApiResponse<SalesDtos.DiscountResponse> addDiscount(@Valid @RequestBody SalesDtos.DiscountRequest req) {
        return ApiResponse.ok(salesService.addDiscount(req));
    }

    @DeleteMapping("/discounts/{id}")
    @Operation(summary = "Delete a discount preset")
    public ApiResponse<Void> deleteDiscount(@PathVariable Long id) {
        salesService.deleteDiscount(id);
        return ApiResponse.ok(null);
    }
}

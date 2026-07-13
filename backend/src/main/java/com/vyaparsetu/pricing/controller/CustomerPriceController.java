package com.vyaparsetu.pricing.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.pricing.service.CustomerPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/distributor/customer-prices")
@Tag(name = "Customer Pricing", description = "Customer-specific price overrides")
@PreAuthorize("hasRole('SUPPLIER')")
public class CustomerPriceController {

    private final CustomerPriceService service;

    public CustomerPriceController(CustomerPriceService service) {
        this.service = service;
    }

    @GetMapping("/retailer/{retailerId}")
    @Operation(summary = "List customer-specific prices for a retailer")
    public ApiResponse<List<CustomerPriceService.CustomerPriceView>> list(@PathVariable Long retailerId) {
        return ApiResponse.ok(service.listForRetailer(retailerId));
    }

    @PutMapping
    @Operation(summary = "Create or update a customer-specific price")
    public ApiResponse<CustomerPriceService.CustomerPriceView> upsert(
            @RequestBody CustomerPriceService.UpsertRequest req) {
        return ApiResponse.ok(service.upsert(req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a customer-specific price")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}

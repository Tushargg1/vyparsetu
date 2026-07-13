package com.vyaparsetu.payment.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.payment.service.CreditAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/distributor/credit")
@Tag(name = "Credit", description = "Distributor-managed retailer credit lines")
@PreAuthorize("hasRole('SUPPLIER')")
public class CreditAdminController {

    private final CreditAdminService service;

    public CreditAdminController(CreditAdminService service) {
        this.service = service;
    }

    @GetMapping("/retailer/{retailerId}")
    @Operation(summary = "Get a retailer's credit line")
    public ApiResponse<CreditAdminService.CreditView> get(@PathVariable Long retailerId) {
        return ApiResponse.ok(service.get(retailerId));
    }

    @PutMapping("/retailer/{retailerId}")
    @Operation(summary = "Set a retailer's credit limit and/or approval")
    public ApiResponse<CreditAdminService.CreditView> setLimit(
            @PathVariable Long retailerId, @RequestBody CreditAdminService.SetLimitRequest req) {
        return ApiResponse.ok(service.setLimit(retailerId, req));
    }

    @PostMapping("/retailer/{retailerId}/status")
    @Operation(summary = "Activate or suspend a retailer's credit account")
    public ApiResponse<CreditAdminService.CreditView> setStatus(
            @PathVariable Long retailerId, @RequestBody CreditAdminService.StatusRequest req) {
        return ApiResponse.ok(service.setStatus(retailerId, req));
    }
}

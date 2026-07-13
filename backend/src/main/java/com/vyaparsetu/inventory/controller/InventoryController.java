package com.vyaparsetu.inventory.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.inventory.dto.InventoryItemResponse;
import com.vyaparsetu.inventory.dto.StockMovementRequest;
import com.vyaparsetu.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory", description = "Retailer inventory and stock movements")
@PreAuthorize("hasRole('RETAILER')")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "List my inventory")
    public ApiResponse<List<InventoryItemResponse>> myInventory() {
        return ApiResponse.ok(inventoryService.myInventory());
    }

    @GetMapping("/low-stock")
    @Operation(summary = "List items at or below reorder level")
    public ApiResponse<List<InventoryItemResponse>> lowStock() {
        return ApiResponse.ok(inventoryService.myLowStock());
    }

    @PostMapping("/movements")
    @Operation(summary = "Apply a stock movement (purchase, sale, return, adjustment, expiry)")
    public ApiResponse<InventoryItemResponse> applyMovement(@Valid @RequestBody StockMovementRequest req) {
        return ApiResponse.ok(inventoryService.applyMovement(req));
    }

    public record ScanSaleRequest(@jakarta.validation.constraints.NotBlank String barcode,
                                  java.math.BigDecimal quantity) {
    }

    @PostMapping("/scan-sale")
    @Operation(summary = "Scan a product barcode to record a customer sale (auto-decrements stock)")
    public ApiResponse<InventoryItemResponse> scanSale(@Valid @RequestBody ScanSaleRequest req) {
        return ApiResponse.ok(inventoryService.sellByBarcode(req.barcode(), req.quantity()));
    }

    @GetMapping("/expiring")
    @Operation(summary = "Stock expiring within N days (default 30)")
    public ApiResponse<List<InventoryService.ExpiringBatch>> expiring(
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(inventoryService.expiringSoon(days));
    }
}

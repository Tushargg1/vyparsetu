package com.vyaparsetu.procurement.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.procurement.entity.ProcurementCampaign;
import com.vyaparsetu.procurement.entity.ProcurementCommitment;
import com.vyaparsetu.procurement.service.ProcurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/procurement")
@Tag(name = "Procurement", description = "Bulk procurement (Phase 3, disabled by default)")
public class ProcurementController {

    private final ProcurementService procurementService;

    public ProcurementController(ProcurementService procurementService) {
        this.procurementService = procurementService;
    }

    public record CreateCampaignRequest(@NotNull Long productId, @NotNull @Positive BigDecimal targetQuantity,
                                        BigDecimal expectedPrice, Instant closesAt) {
    }

    public record JoinRequest(@NotNull @Positive BigDecimal quantity, BigDecimal advancePaid) {
    }

    @GetMapping("/campaigns")
    @Operation(summary = "List open bulk procurement campaigns")
    public ApiResponse<List<ProcurementCampaign>> openCampaigns() {
        return ApiResponse.ok(procurementService.openCampaigns());
    }

    @PostMapping("/campaigns")
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    public ApiResponse<ProcurementCampaign> create(@RequestBody CreateCampaignRequest req) {
        return ApiResponse.ok(procurementService.createCampaign(
                req.productId(), req.targetQuantity(), req.expectedPrice(), req.closesAt()));
    }

    @PostMapping("/campaigns/{id}/join")
    @PreAuthorize("hasRole('RETAILER')")
    @Operation(summary = "Join a bulk procurement campaign with an advance")
    public ApiResponse<ProcurementCommitment> join(@PathVariable Long id, @RequestBody JoinRequest req) {
        return ApiResponse.ok(procurementService.join(id, req.quantity(),
                req.advancePaid() != null ? req.advancePaid() : BigDecimal.ZERO));
    }
}

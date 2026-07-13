package com.vyaparsetu.user.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.user.dto.NetworkDtos;
import com.vyaparsetu.user.service.NetworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/distributor")
@Tag(name = "Distributor", description = "Distributor network: invite code, retailers, WhatsApp")
@PreAuthorize("hasRole('SUPPLIER')")
public class DistributorController {

    private final NetworkService networkService;

    public DistributorController(NetworkService networkService) {
        this.networkService = networkService;
    }

    @GetMapping("/invite-code")
    @Operation(summary = "Get my unique invite code and WhatsApp join link")
    public ApiResponse<NetworkDtos.InviteCodeResponse> inviteCode() {
        return ApiResponse.ok(networkService.myInviteCode());
    }

    @GetMapping("/retailers")
    @Operation(summary = "List the retailers linked to me")
    public ApiResponse<List<NetworkDtos.RetailerSummary>> retailers() {
        return ApiResponse.ok(networkService.myRetailers());
    }

    @PostMapping("/retailers")
    @Operation(summary = "Add a retailer (pre-linked to me); sends them a login OTP")
    public ApiResponse<NetworkDtos.RetailerSummary> addRetailer(
            @Valid @RequestBody NetworkDtos.AddRetailerRequest req) {
        return ApiResponse.ok(networkService.addRetailer(req));
    }

    @PostMapping("/retailers/{retailerId}/payments")
    @Operation(summary = "Record a payment received from a retailer (applied to oldest dues first)")
    public ApiResponse<NetworkDtos.RecordPaymentResult> recordPayment(
            @PathVariable Long retailerId,
            @Valid @RequestBody NetworkDtos.RecordPaymentRequest req) {
        return ApiResponse.ok(networkService.recordRetailerPayment(retailerId, req.amount()));
    }

    @PutMapping("/whatsapp")
    @Operation(summary = "Link / update my WhatsApp number")
    public ApiResponse<Void> setWhatsApp(@Valid @RequestBody NetworkDtos.WhatsAppSettingsRequest req) {
        networkService.setWhatsApp(req);
        return ApiResponse.ok(null);
    }

    @GetMapping("/profile")
    @Operation(summary = "Get my distributor profile (name, shop, numbers, address, location)")
    public ApiResponse<NetworkDtos.MyProfileResponse> profile() {
        return ApiResponse.ok(networkService.mySupplierProfile());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update my distributor profile")
    public ApiResponse<NetworkDtos.MyProfileResponse> updateProfile(@RequestBody NetworkDtos.ProfileRequest req) {
        return ApiResponse.ok(networkService.updateSupplierProfile(req));
    }
}

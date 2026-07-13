package com.vyaparsetu.whatsapp.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.whatsapp.WhatsAppEnums;
import com.vyaparsetu.whatsapp.dto.WhatsAppDtos;
import com.vyaparsetu.whatsapp.service.WhatsAppAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Distributor-facing WhatsApp AI control panel: connection, AI settings,
 * human takeover, and customer onboarding approvals.
 */
@RestController
@RequestMapping("/api/v1/distributor/whatsapp")
@Tag(name = "WhatsApp AI", description = "Distributor WhatsApp AI assistant management")
@PreAuthorize("hasRole('SUPPLIER')")
public class WhatsAppAdminController {

    private final WhatsAppAdminService service;

    public WhatsAppAdminController(WhatsAppAdminService service) {
        this.service = service;
    }

    @GetMapping("/settings")
    @Operation(summary = "Get WhatsApp connection + AI settings")
    public ApiResponse<WhatsAppDtos.SettingsResponse> getSettings() {
        return ApiResponse.ok(service.getSettings());
    }

    @PutMapping("/settings")
    @Operation(summary = "Update AI settings (partial)")
    public ApiResponse<WhatsAppDtos.SettingsResponse> updateSettings(@RequestBody WhatsAppDtos.SettingsUpdateRequest req) {
        return ApiResponse.ok(service.updateSettings(req));
    }

    @PostMapping("/connect")
    @Operation(summary = "Connect a WhatsApp business number")
    public ApiResponse<WhatsAppDtos.SettingsResponse> connect(@Valid @RequestBody WhatsAppDtos.ConnectRequest req) {
        return ApiResponse.ok(service.connect(req.businessNumber()));
    }

    @PostMapping("/disconnect")
    @Operation(summary = "Disconnect WhatsApp")
    public ApiResponse<WhatsAppDtos.SettingsResponse> disconnect() {
        return ApiResponse.ok(service.disconnect());
    }

    @PostMapping("/takeover")
    @Operation(summary = "Toggle human takeover (pauses the AI)")
    public ApiResponse<WhatsAppDtos.SettingsResponse> takeover(@RequestBody WhatsAppDtos.TakeoverRequest req) {
        return ApiResponse.ok(service.setTakeover(req.enabled()));
    }

    @PostMapping("/simulate")
    @Operation(summary = "Test the assistant with a simulated inbound customer message")
    public ApiResponse<Map<String, String>> simulate(@Valid @RequestBody WhatsAppDtos.SimulateRequest req) {
        String reply = service.simulate(req.from(), req.text());
        return ApiResponse.ok(Map.of("reply", reply == null ? "" : reply));
    }

    @GetMapping("/requests")
    @Operation(summary = "List customer onboarding requests")
    public ApiResponse<List<WhatsAppDtos.RetailerRequestResponse>> requests(
            @RequestParam(required = false) WhatsAppEnums.RequestStatus status) {
        return ApiResponse.ok(service.listRequests(status));
    }

    @PostMapping("/requests/{id}/approve")
    @Operation(summary = "Approve a customer onboarding request")
    public ApiResponse<Void> approve(@PathVariable Long id) {
        service.approveRequest(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/requests/{id}/reject")
    @Operation(summary = "Reject a customer onboarding request")
    public ApiResponse<Void> reject(@PathVariable Long id) {
        service.rejectRequest(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/retailers/{retailerId}/numbers")
    @Operation(summary = "List linked WhatsApp numbers for a retailer")
    public ApiResponse<List<WhatsAppDtos.LinkedNumberResponse>> numbers(@PathVariable Long retailerId) {
        return ApiResponse.ok(service.linkedNumbers(retailerId));
    }

    @PostMapping("/retailers/{retailerId}/numbers")
    @Operation(summary = "Link an additional WhatsApp number (sends an OTP to it)")
    public ApiResponse<WhatsAppDtos.LinkedNumberResponse> addNumber(@PathVariable Long retailerId,
                                                                    @Valid @RequestBody WhatsAppDtos.AddNumberRequest req) {
        return ApiResponse.ok(service.addNumber(retailerId, req.phone()));
    }

    @PostMapping("/retailers/{retailerId}/numbers/verify")
    @Operation(summary = "Verify a linked WhatsApp number with its OTP")
    public ApiResponse<WhatsAppDtos.LinkedNumberResponse> verifyNumber(@PathVariable Long retailerId,
                                                                       @Valid @RequestBody WhatsAppDtos.VerifyNumberRequest req) {
        return ApiResponse.ok(service.verifyNumber(retailerId, req.phone(), req.code()));
    }

    @DeleteMapping("/retailers/{retailerId}/numbers/{numberId}")
    @Operation(summary = "Remove a linked WhatsApp number")
    public ApiResponse<Void> removeNumber(@PathVariable Long retailerId, @PathVariable Long numberId) {
        service.removeNumber(retailerId, numberId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/aliases")
    @Operation(summary = "List product aliases")
    public ApiResponse<List<WhatsAppDtos.AliasResponse>> aliases() {
        return ApiResponse.ok(service.listAliases());
    }

    @PostMapping("/aliases")
    @Operation(summary = "Add a product alias (e.g. Coke → Coca-Cola)")
    public ApiResponse<WhatsAppDtos.AliasResponse> addAlias(@Valid @RequestBody WhatsAppDtos.AddAliasRequest req) {
        return ApiResponse.ok(service.addAlias(req));
    }

    @DeleteMapping("/aliases/{id}")
    @Operation(summary = "Delete a product alias")
    public ApiResponse<Void> deleteAlias(@PathVariable Long id) {
        service.deleteAlias(id);
        return ApiResponse.ok(null);
    }
}

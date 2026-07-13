package com.vyaparsetu.user.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.user.dto.NetworkDtos;
import com.vyaparsetu.user.service.NetworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/retailer")
@Tag(name = "Retailer Network", description = "Retailer joining and viewing their distributor")
@PreAuthorize("hasRole('RETAILER')")
public class RetailerNetworkController {

    private final NetworkService networkService;

    public RetailerNetworkController(NetworkService networkService) {
        this.networkService = networkService;
    }

    @PostMapping("/join")
    @Operation(summary = "Join a distributor using their invite code")
    public ApiResponse<NetworkDtos.DistributorResponse> join(@Valid @RequestBody NetworkDtos.JoinRequest req) {
        return ApiResponse.ok(networkService.joinByCode(req.inviteCode()));
    }

    @GetMapping("/distributor")
    @Operation(summary = "Get my distributor")
    public ApiResponse<NetworkDtos.DistributorResponse> myDistributor() {
        return ApiResponse.ok(networkService.myDistributor());
    }

    @GetMapping("/profile")
    @Operation(summary = "Get my retailer profile (name, shop, numbers, address, location)")
    public ApiResponse<NetworkDtos.MyProfileResponse> profile() {
        return ApiResponse.ok(networkService.myRetailerProfile());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update my retailer profile")
    public ApiResponse<NetworkDtos.MyProfileResponse> updateProfile(@RequestBody NetworkDtos.ProfileRequest req) {
        return ApiResponse.ok(networkService.updateRetailerProfile(req));
    }
}

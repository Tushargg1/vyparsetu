package com.vyaparsetu.user.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.user.dto.NetworkDtos;
import com.vyaparsetu.user.service.NetworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/distributors")
@Tag(name = "Distributor Directory", description = "Browse all distributors to order from")
public class DistributorDirectoryController {

    private final NetworkService networkService;

    public DistributorDirectoryController(NetworkService networkService) {
        this.networkService = networkService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RETAILER','ADMIN')")
    @Operation(summary = "List all distributors")
    public ApiResponse<List<NetworkDtos.DistributorResponse>> all() {
        return ApiResponse.ok(networkService.listDistributors());
    }
}

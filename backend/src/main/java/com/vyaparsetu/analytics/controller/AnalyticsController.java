package com.vyaparsetu.analytics.controller;

import com.vyaparsetu.analytics.service.AnalyticsService;
import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/distributor/analytics")
@Tag(name = "Distributor Analytics", description = "Aggregated ordering metrics")
@PreAuthorize("hasRole('SUPPLIER')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserService userService;

    public AnalyticsController(AnalyticsService analyticsService, UserService userService) {
        this.analyticsService = analyticsService;
        this.userService = userService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Aggregated analytics summary for the distributor")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok(analyticsService.summary(userService.currentSupplierId()));
    }
}

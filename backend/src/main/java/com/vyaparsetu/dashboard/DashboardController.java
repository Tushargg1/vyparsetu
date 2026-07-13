package com.vyaparsetu.dashboard;

import com.vyaparsetu.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Role dashboards")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/retailer")
    @PreAuthorize("hasRole('RETAILER')")
    @Operation(summary = "Retailer dashboard: today's sales/profit and key insights")
    public ApiResponse<DashboardService.RetailerDashboard> retailer() {
        return ApiResponse.ok(dashboardService.retailerDashboard());
    }
}

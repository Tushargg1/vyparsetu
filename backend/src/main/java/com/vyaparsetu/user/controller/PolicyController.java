package com.vyaparsetu.user.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.user.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/distributor/policy")
@Tag(name = "Distributor Policy", description = "Configurable ordering policies")
@PreAuthorize("hasRole('SUPPLIER')")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    @Operation(summary = "Get ordering policies")
    public ApiResponse<PolicyService.PolicyView> get() {
        return ApiResponse.ok(policyService.get());
    }

    @PutMapping
    @Operation(summary = "Update ordering policies")
    public ApiResponse<PolicyService.PolicyView> update(@RequestBody PolicyService.PolicyUpdate req) {
        return ApiResponse.ok(policyService.update(req));
    }
}

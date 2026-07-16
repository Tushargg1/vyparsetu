package com.vyaparsetu.dev;

import com.vyaparsetu.auth.dto.AuthTokenResponse;
import com.vyaparsetu.common.enums.RoleName;
import com.vyaparsetu.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/demo-login")
@Tag(name = "Demo", description = "Shared synthetic retailer and distributor demos")
@ConditionalOnProperty(name = "app.features.demo-login.enabled", havingValue = "true")
public class DemoController {

    private final DevSeedService seedService;

    public DemoController(DevSeedService seedService) {
        this.seedService = seedService;
    }

    public enum DemoRole { RETAILER, SUPPLIER }

    public record DemoLoginRequest(@NotNull DemoRole role) {
    }

    @PostMapping
    @Operation(summary = "Enter a shared retailer or distributor demo")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody DemoLoginRequest req) {
        RoleName role = req.role() == DemoRole.RETAILER ? RoleName.RETAILER : RoleName.SUPPLIER;
        return ApiResponse.ok(seedService.loginDemo(role));
    }
}
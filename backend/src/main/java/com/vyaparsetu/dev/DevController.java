package com.vyaparsetu.dev;

import com.vyaparsetu.auth.dto.AuthTokenResponse;
import com.vyaparsetu.common.enums.RoleName;
import com.vyaparsetu.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * DEV ONLY. Test-login endpoints used by the UI "Quick test login"
 * buttons. This bean only exists when the dev profile is explicitly active.
 */
@RestController
@RequestMapping("/api/v1/auth/dev-login")
@Tag(name = "Dev", description = "Test logins (development only)")
@Profile("dev")
public class DevController {

    private final DevSeedService devSeedService;

    public DevController(DevSeedService devSeedService) {
        this.devSeedService = devSeedService;
    }

    public record DevLoginRequest(@NotNull RoleName role) {
    }

    @PostMapping
    @Operation(summary = "Seed test data and log in as RETAILER / SUPPLIER / ADMIN")
    public ApiResponse<AuthTokenResponse> login(@RequestBody DevLoginRequest req) {
        return ApiResponse.ok(devSeedService.login(req.role()));
    }
}

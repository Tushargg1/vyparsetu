package com.vyaparsetu.payment.controller;

import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.payment.entity.Wallet;
import com.vyaparsetu.payment.service.WalletService;
import com.vyaparsetu.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/wallet")
@Tag(name = "Wallet", description = "Retailer wallet")
@PreAuthorize("hasRole('RETAILER')")
@Validated
public class WalletController {

    private final WalletService walletService;
    private final UserService userService;
    private final Environment environment;

    public WalletController(WalletService walletService, UserService userService, Environment environment) {
        this.walletService = walletService;
        this.userService = userService;
        this.environment = environment;
    }

    @GetMapping
    @Operation(summary = "Get my wallet balance")
    public ApiResponse<Wallet> myWallet() {
        return ApiResponse.ok(walletService.getOrCreate(userService.currentRetailerId()));
    }

    @PostMapping("/topup")
    @Operation(summary = "Top up wallet (simulated; non-production only)")
    public ApiResponse<Wallet> topUp(@RequestParam @Positive BigDecimal amount) {
        // SECURITY: this is a simulated top-up with no real payment. It must never be
        // reachable in production, where top-ups must flow through the payment gateway.
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            throw new BusinessException("TOPUP_DISABLED", HttpStatus.FORBIDDEN,
                    "Wallet top-up must go through the payment gateway");
        }
        return ApiResponse.ok(walletService.credit(
                userService.currentRetailerId(), amount, null, "Wallet top-up (simulated)"));
    }
}

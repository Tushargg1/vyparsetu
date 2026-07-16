package com.vyaparsetu.auth.controller;

import com.vyaparsetu.auth.dto.*;
import com.vyaparsetu.auth.service.AuthService;
import com.vyaparsetu.auth.service.PasskeyService;
import com.vyaparsetu.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "OTP-based registration, login and token management")
public class AuthController {

    private final AuthService authService;
    private final PasskeyService passkeyService;

    public AuthController(AuthService authService, PasskeyService passkeyService) {
        this.authService = authService;
        this.passkeyService = passkeyService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user and send a verification OTP")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null));
    }

    @PostMapping("/otp/send")
    @Operation(summary = "Send an OTP for login/registration/reset")
    public ApiResponse<Void> sendOtp(@Valid @RequestBody SendOtpRequest req) {
        authService.sendOtp(req);
        return ApiResponse.ok(null);
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify an OTP and receive access/refresh tokens")
    public ApiResponse<AuthTokenResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest req,
                                                    HttpServletRequest http) {
        return ApiResponse.ok(authService.verifyOtp(req, http.getHeader("User-Agent")));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue a new access token")
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshRequest req,
                                                  HttpServletRequest http) {
        return ApiResponse.ok(authService.refresh(req, http.getHeader("User-Agent")));
    }

    @PostMapping("/totp/options")
    @Operation(summary = "Start authenticator-app sign-in")
    public ApiResponse<TotpChallengeResponse> totpOptions(
            @Valid @RequestBody TotpOptionsRequest req) {
        return ApiResponse.ok(authService.startTotpLogin(req));
    }

    @PostMapping("/totp/verify")
    @Operation(summary = "Complete sign-in with an authenticator-app code")
    public ApiResponse<AuthTokenResponse> verifyTotp(@Valid @RequestBody TotpLoginRequest req,
                                                     HttpServletRequest http) {
        return ApiResponse.ok(authService.verifyTotp(req, http.getHeader("User-Agent")));
    }

    @PostMapping("/passkeys/options")
    @Operation(summary = "Start passkey authentication")
    public ApiResponse<PasskeyOptionsResponse> passkeyOptions(
            @Valid @RequestBody PasskeyOptionsRequest req) {
        return ApiResponse.ok(passkeyService.startAuthentication(req.identifier()));
    }

    @PostMapping("/passkeys/verify")
    @Operation(summary = "Verify a passkey and issue tokens")
    public ApiResponse<AuthTokenResponse> verifyPasskey(@Valid @RequestBody PasskeyFinishRequest req,
                                                        HttpServletRequest http) {
        Long userId = passkeyService.finishAuthentication(req);
        return ApiResponse.ok(authService.issueTokensAfterStrongAuth(
                userId, http.getHeader("User-Agent"), "PASSKEY"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest req) {
        authService.logout(req);
        return ApiResponse.ok(null);
    }
}

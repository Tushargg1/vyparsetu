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
@Tag(name = "Authentication", description = "Password, passkey and authenticator sign-in")
public class AuthController {

    private final AuthService authService;
    private final PasskeyService passkeyService;

    public AuthController(AuthService authService, PasskeyService passkeyService) {
        this.authService = authService;
        this.passkeyService = passkeyService;
    }

    @PostMapping("/register")
    @Operation(summary = "Create an email/password account")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> register(
            @Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        AuthTokenResponse tokens = authService.register(req, http.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(tokens));
    }

    @PostMapping("/password/login")
    @Operation(summary = "Sign in with email and password")
    public ApiResponse<AuthTokenResponse> passwordLogin(
            @Valid @RequestBody PasswordLoginRequest req, HttpServletRequest http) {
        return ApiResponse.ok(authService.loginWithPassword(req, http.getHeader("User-Agent")));
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

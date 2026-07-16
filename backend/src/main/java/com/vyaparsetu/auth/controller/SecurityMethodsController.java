package com.vyaparsetu.auth.controller;

import com.vyaparsetu.auth.dto.*;
import com.vyaparsetu.auth.service.PasskeyService;
import com.vyaparsetu.auth.service.TotpService;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.common.security.CurrentUser;
import com.vyaparsetu.user.entity.User;
import com.vyaparsetu.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/security")
public class SecurityMethodsController {
    private final PasskeyService passkeys;
    private final TotpService totp;
    private final UserRepository users;

    public SecurityMethodsController(PasskeyService passkeys, TotpService totp, UserRepository users) {
        this.passkeys = passkeys;
        this.totp = totp;
        this.users = users;
    }

    @GetMapping
    public ApiResponse<SecurityMethodsResponse> status() {
        Long userId = CurrentUser.id();
        return ApiResponse.ok(passkeys.status(userId, totp.isEnabled(userId)));
    }

    @PostMapping("/passkeys/options")
    public ApiResponse<PasskeyOptionsResponse> passkeyOptions() {
        CurrentUser.requireRecentAuthentication();
        return ApiResponse.ok(passkeys.startRegistration(CurrentUser.id()));
    }

    @PostMapping("/passkeys/verify")
    public ApiResponse<PasskeySummary> verifyPasskey(@Valid @RequestBody PasskeyFinishRequest req) {
        return ApiResponse.ok(passkeys.finishRegistration(CurrentUser.id(), req));
    }

    @DeleteMapping("/passkeys/{id}")
    public ApiResponse<Void> deletePasskey(@PathVariable Long id) {
        CurrentUser.requireRecentAuthentication();
        passkeys.delete(CurrentUser.id(), id);
        return ApiResponse.ok(null);
    }
    @PostMapping("/totp/setup")
    public ApiResponse<TotpSetupResponse> setupTotp() {
        CurrentUser.requireRecentAuthentication();
        Long userId = CurrentUser.id();
        User user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return ApiResponse.ok(totp.setup(user));
    }

    @PostMapping("/totp/confirm")
    public ApiResponse<Void> confirmTotp(@Valid @RequestBody TotpCodeRequest req) {
        totp.confirm(CurrentUser.id(), req.code());
        return ApiResponse.ok(null);
    }

    @PostMapping("/totp/disable")
    public ApiResponse<Void> disableTotp(@Valid @RequestBody TotpCodeRequest req) {
        totp.disable(CurrentUser.id(), req.code());
        return ApiResponse.ok(null);
    }
}

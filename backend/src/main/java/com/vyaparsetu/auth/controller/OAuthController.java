package com.vyaparsetu.auth.controller;

import com.vyaparsetu.auth.dto.AuthTokenResponse;
import com.vyaparsetu.auth.dto.OAuthExchangeRequest;
import com.vyaparsetu.auth.service.OAuthService;
import com.vyaparsetu.common.enums.RoleName;
import com.vyaparsetu.common.exception.BaseException;
import com.vyaparsetu.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth/oauth")
public class OAuthController {
    private final OAuthService oauth;

    public OAuthController(OAuthService oauth) {
        this.oauth = oauth;
    }

    @GetMapping("/providers")
    public ApiResponse<OAuthService.ProviderStatus> providers() {
        return ApiResponse.ok(oauth.providers());
    }

    @GetMapping("/{provider}/start")
    public ResponseEntity<Void> start(@PathVariable String provider,
                                      @RequestParam RoleName role) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(oauth.start(provider, role))).build();
    }

    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callbackGet(@PathVariable String provider,
                                             @RequestParam(required = false) String code,
                                             @RequestParam(required = false) String state,
                                             @RequestParam(required = false) String error) {
        return callback(provider, code, state, error);
    }

    @PostMapping("/{provider}/callback")
    public ResponseEntity<Void> callbackPost(@PathVariable String provider,
                                              @RequestParam(required = false) String code,
                                              @RequestParam(required = false) String state,
                                              @RequestParam(required = false) String error) {
        return callback(provider, code, state, error);
    }

    @PostMapping("/exchange")
    public ApiResponse<AuthTokenResponse> exchange(@Valid @RequestBody OAuthExchangeRequest req,
                                                    HttpServletRequest http) {
        return ApiResponse.ok(oauth.exchange(req.code(), http.getHeader("User-Agent")));
    }

    private ResponseEntity<Void> callback(String provider, String code, String state, String error) {
        String redirect;
        try {
            if (error != null || code == null || state == null) {
                redirect = oauth.errorRedirect("provider_cancelled");
            } else {
                redirect = oauth.callback(provider, code, state);
            }
        } catch (BaseException ex) {
            redirect = oauth.errorRedirect(ex.getCode().toLowerCase());
        } catch (Exception ex) {
            redirect = oauth.errorRedirect("oauth_failed");
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirect).build();
    }
}
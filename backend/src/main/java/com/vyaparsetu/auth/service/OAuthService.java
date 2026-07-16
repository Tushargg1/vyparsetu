package com.vyaparsetu.auth.service;

import com.vyaparsetu.auth.config.OAuthProperties;
import com.vyaparsetu.auth.dto.AuthTokenResponse;
import com.vyaparsetu.auth.entity.ExternalIdentity;
import com.vyaparsetu.auth.entity.OAuthLoginCode;
import com.vyaparsetu.auth.repository.ExternalIdentityRepository;
import com.vyaparsetu.auth.repository.OAuthLoginCodeRepository;
import com.vyaparsetu.common.config.AppProperties;
import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.enums.RoleName;
import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.user.entity.User;
import com.vyaparsetu.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class OAuthService {
    private final OAuthProperties oauth;
    private final ExternalIdentityRepository identities;
    private final OAuthLoginCodeRepository loginCodes;
    private final UserRepository users;
    private final AuthService authService;
    private final SecretKey stateKey;
    private final SecureRandom random = new SecureRandom();
    private final RestClient rest = RestClient.create();

    public OAuthService(OAuthProperties oauth, ExternalIdentityRepository identities,
                        OAuthLoginCodeRepository loginCodes, UserRepository users,
                        AuthService authService, AppProperties app) {
        this.oauth = oauth;
        this.identities = identities;
        this.loginCodes = loginCodes;
        this.users = users;
        this.authService = authService;
        this.stateKey = Keys.hmacShaKeyFor(app.getSecurity().getJwt().getSecret()
                .getBytes(StandardCharsets.UTF_8));
    }

    public record ProviderStatus(boolean google, boolean apple) {}

    public ProviderStatus providers() {
        return new ProviderStatus(oauth.getGoogle().isConfigured(), oauth.getApple().isConfigured());
    }

    public String start(String providerName, RoleName role) {
        Provider provider = Provider.parse(providerName);
        if (role != RoleName.RETAILER && role != RoleName.SUPPLIER) {
            throw new BusinessException("OAUTH_ROLE_NOT_ALLOWED", HttpStatus.BAD_REQUEST,
                    "Choose retailer or supplier");
        }
        OAuthProperties.Provider config = config(provider);
        requireConfigured(provider, config);
        String nonce = randomToken(24);
        Instant now = Instant.now();
        String state = Jwts.builder()
                .subject("oauth-state")
                .claim("provider", provider.name())
                .claim("role", role.name())
                .claim("nonce", nonce)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(10, ChronoUnit.MINUTES)))
                .signWith(stateKey)
                .compact();
        UriComponentsBuilder url = UriComponentsBuilder.fromUriString(config.getAuthorizationUri())
                .queryParam("client_id", config.getClientId())
                .queryParam("redirect_uri", config.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email")
                .queryParam("state", state)
                .queryParam("nonce", nonce);
        if (provider == Provider.GOOGLE) url.queryParam("prompt", "select_account");
        if (provider == Provider.APPLE) url.queryParam("response_mode", "form_post");
        return url.build().encode().toUriString();
    }

    @Transactional
    public String callback(String providerName, String code, String state) {
        Provider provider = Provider.parse(providerName);
        OAuthProperties.Provider config = config(provider);
        requireConfigured(provider, config);
        Claims stateClaims = Jwts.parser().verifyWith(stateKey).build()
                .parseSignedClaims(state).getPayload();
        if (!"oauth-state".equals(stateClaims.getSubject())
                || !provider.name().equals(stateClaims.get("provider", String.class))) {
            throw new BusinessException("INVALID_OAUTH_STATE", HttpStatus.UNAUTHORIZED,
                    "OAuth state is invalid or expired");
        }
        RoleName requestedRole = RoleName.valueOf(stateClaims.get("role", String.class));
        String expectedNonce = stateClaims.get("nonce", String.class);
        Map<?, ?> token = exchangeProviderCode(config, code);
        String idToken = Objects.toString(token.get("id_token"), "");
        if (idToken.isBlank()) {
            throw new BusinessException("OAUTH_TOKEN_MISSING", HttpStatus.UNAUTHORIZED,
                    "The identity provider did not return an ID token");
        }
        Jwt jwt = JwtDecoders.fromIssuerLocation(config.getIssuerUri()).decode(idToken);
        if (!jwt.getAudience().contains(config.getClientId())
                || !Objects.equals(expectedNonce, jwt.getClaimAsString("nonce"))) {
            throw new BusinessException("INVALID_OAUTH_TOKEN", HttpStatus.UNAUTHORIZED,
                    "The identity token could not be verified");
        }
        String subject = jwt.getSubject();
        String email = normalizeEmail(jwt.getClaimAsString("email"));
        if (subject == null || email.isBlank() || !isEmailVerified(jwt.getClaims().get("email_verified"))) {
            throw new BusinessException("OAUTH_EMAIL_REQUIRED", HttpStatus.BAD_REQUEST,
                    "A verified provider email is required");
        }

        ExternalIdentity identity = identities.findByProviderAndProviderSubject(provider.name(), subject)
                .orElse(null);
        User user = identity == null
                ? users.findByEmailIgnoreCase(email).orElseThrow(() ->
                    new BusinessException("OAUTH_ACCOUNT_REQUIRED", HttpStatus.CONFLICT,
                            "Create an account with this email before using social sign-in"))
                : users.findById(identity.getUserId()).orElseThrow(() ->
                    new BusinessException("OAUTH_ACCOUNT_MISSING", HttpStatus.UNAUTHORIZED,
                            "The linked account no longer exists"));
        boolean roleMatches = user.getRoles().stream().anyMatch(r -> r.getName() == requestedRole);
        if (!roleMatches || user.getStatus() != Enums.UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw new BusinessException("OAUTH_ACCOUNT_UNAVAILABLE", HttpStatus.FORBIDDEN,
                    "This account is unavailable for the selected workspace");
        }

        if (identity == null) {
            identity = new ExternalIdentity();
            identity.setUserId(user.getId());
            identity.setProvider(provider.name());
            identity.setProviderSubject(subject);
        }
        identity.setEmail(email);
        identity.setLastLoginAt(Instant.now());
        identities.save(identity);

        String rawCode = randomToken(32);
        OAuthLoginCode loginCode = new OAuthLoginCode();
        loginCode.setCodeHash(sha256(rawCode));
        loginCode.setUserId(user.getId());
        loginCode.setProvider(provider.name());
        loginCode.setExpiresAt(Instant.now().plus(2, ChronoUnit.MINUTES));
        loginCodes.save(loginCode);
        return UriComponentsBuilder.fromUriString(oauth.getFrontendUrl())
                .path("/login").queryParam("oauthCode", rawCode).build().encode().toUriString();
    }

    @Transactional
    public AuthTokenResponse exchange(String rawCode, String deviceInfo) {
        OAuthLoginCode code = loginCodes.findUsableForUpdate(sha256(rawCode))
                .orElseThrow(() -> new BusinessException("INVALID_OAUTH_CODE", HttpStatus.UNAUTHORIZED,
                        "OAuth sign-in code is invalid"));
        if (code.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("INVALID_OAUTH_CODE", HttpStatus.UNAUTHORIZED,
                    "OAuth sign-in code expired");
        }
        code.setConsumedAt(Instant.now());
        loginCodes.save(code);
        return authService.issueOAuthTokens(code.getUserId(), deviceInfo, code.getProvider());
    }

    public String errorRedirect(String code) {
        return UriComponentsBuilder.fromUriString(oauth.getFrontendUrl())
                .path("/login").queryParam("oauthError", code).build().encode().toUriString();
    }

    private Map<?, ?> exchangeProviderCode(OAuthProperties.Provider config, String code) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", config.getClientId());
        form.add("client_secret", config.getClientSecret());
        form.add("redirect_uri", config.getRedirectUri());
        Map<?, ?> response = rest.post().uri(config.getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(Map.class);
        return response == null ? Map.of() : response;
    }

    private OAuthProperties.Provider config(Provider provider) {
        return provider == Provider.GOOGLE ? oauth.getGoogle() : oauth.getApple();
    }

    private void requireConfigured(Provider provider, OAuthProperties.Provider config) {
        if (!config.isConfigured()) {
            throw new BusinessException("OAUTH_PROVIDER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    provider.display + " sign-in has not been configured yet");
        }
    }

    private boolean isEmailVerified(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(Objects.toString(value, ""));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String randomToken(int bytes) {
        byte[] value = new byte[bytes];
        random.nextBytes(value);
        return HexFormat.of().formatHex(value);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private enum Provider {
        GOOGLE("Google"), APPLE("Apple");
        private final String display;
        Provider(String display) { this.display = display; }
        static Provider parse(String value) {
            try {
                return Provider.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (Exception ex) {
                throw new BusinessException("OAUTH_PROVIDER_INVALID", HttpStatus.NOT_FOUND,
                        "Unknown OAuth provider");
            }
        }
    }
}
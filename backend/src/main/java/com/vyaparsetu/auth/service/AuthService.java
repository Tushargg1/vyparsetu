package com.vyaparsetu.auth.service;

import com.vyaparsetu.auth.dto.*;
import com.vyaparsetu.auth.entity.RefreshToken;
import com.vyaparsetu.auth.repository.RefreshTokenRepository;
import com.vyaparsetu.common.config.AppProperties;
import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.enums.RoleName;
import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.common.security.JwtTokenProvider;
import com.vyaparsetu.user.dto.UserResponse;
import com.vyaparsetu.user.entity.*;
import com.vyaparsetu.user.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RetailerRepository retailerRepository;
    private final SupplierRepository supplierRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TotpService totpService;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder encoder;
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       RetailerRepository retailerRepository, SupplierRepository supplierRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       TotpService totpService, JwtTokenProvider tokenProvider,
                       PasswordEncoder encoder, AppProperties props) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.retailerRepository = retailerRepository;
        this.supplierRepository = supplierRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.totpService = totpService;
        this.tokenProvider = tokenProvider;
        this.encoder = encoder;
        this.props = props;
    }

    @Transactional
    public AuthTokenResponse register(RegisterRequest req, String deviceInfo) {
        // SECURITY: public self-registration is limited to business-facing roles.
        if (req.role() == null || (req.role() != RoleName.RETAILER && req.role() != RoleName.SUPPLIER)) {
            throw new BusinessException("ROLE_NOT_ALLOWED", HttpStatus.FORBIDDEN,
                    "This role cannot be self-registered");
        }
        String email = normalizeEmail(req.email());
        if (userRepository.existsByPhone(req.phone())) {
            throw new BusinessException("PHONE_TAKEN", HttpStatus.CONFLICT, "Phone already registered");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("EMAIL_TAKEN", HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setName(req.name().trim());
        user.setPhone(req.phone());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(req.password()));
        user.setStatus(Enums.UserStatus.ACTIVE);
        user.setPhoneVerified(false);
        user.setEmailVerified(false);
        if (req.preferredLanguage() != null) {
            user.setPreferredLanguage(req.preferredLanguage());
        }
        Role role = roleRepository.findByName(req.role())
                .orElseThrow(() -> new ResourceNotFoundException("Role", req.role()));
        user.setRoles(Set.of(role));
        user = userRepository.save(user);

        createRoleProfile(user.getId(), req);
        return issueTokens(user, deviceInfo, Instant.now(), "PASSWORD");
    }

    @Transactional
    public AuthTokenResponse loginWithPassword(PasswordLoginRequest req, String deviceInfo) {
        String email = normalizeEmail(req.email());
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || user.getPasswordHash() == null
                || !encoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED,
                    "Invalid email or password");
        }
        if (user.getStatus() != Enums.UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw new BusinessException("ACCOUNT_NOT_ACTIVE", HttpStatus.FORBIDDEN,
                    "Account is not active");
        }
        return issueTokens(user, deviceInfo, Instant.now(), "PASSWORD");
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private void createRoleProfile(Long userId, RegisterRequest req) {
        switch (req.role()) {
            case RETAILER -> {
                Retailer r = new Retailer();
                r.setUserId(userId);
                r.setShopName(req.shopName() != null ? req.shopName() : req.name());
                r.setGstNumber(req.gstNumber());
                r.setAddress(req.address());
                r.setCity(req.city());
                r.setState(req.state());
                r.setPincode(req.pincode());
                r.setAltPhones(req.altPhones());
                r.setLocationUrl(req.locationUrl());
                // Link to a distributor if an invite code was supplied at signup.
                if (req.inviteCode() != null && !req.inviteCode().isBlank()) {
                    Supplier distributor = supplierRepository.findByInviteCode(req.inviteCode().trim())
                            .orElseThrow(() -> new BusinessException("INVALID_INVITE_CODE",
                                    HttpStatus.BAD_REQUEST, "Invalid distributor invite code"));
                    r.setDistributorId(distributor.getId());
                }
                retailerRepository.save(r);
            }
            case SUPPLIER -> {
                if (req.supplierType() == null) {
                    throw new BusinessException("supplierType is required for suppliers");
                }
                Supplier s = new Supplier();
                s.setUserId(userId);
                s.setBusinessName(req.businessName() != null ? req.businessName() : req.name());
                s.setSupplierType(req.supplierType());
                s.setGstNumber(req.gstNumber());
                s.setAddress(req.address());
                s.setCity(req.city());
                s.setState(req.state());
                s.setPincode(req.pincode());
                s.setAltPhones(req.altPhones());
                s.setLocationUrl(req.locationUrl());
                s.setInviteCode(generateUniqueInviteCode());
                supplierRepository.save(s);
            }
            case ADMIN -> {
                // admin has no extra profile
            }
        }
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = com.vyaparsetu.common.util.NumberGenerator.inviteCode();
        } while (supplierRepository.existsByInviteCode(code));
        return code;
    }

    @Transactional
    public TotpChallengeResponse startTotpLogin(TotpOptionsRequest req) {
        String identifier = req.identifier().trim();
        User user = userRepository.findByPhone(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                .filter(value -> value.getStatus() == Enums.UserStatus.ACTIVE)
                .filter(value -> value.getDeletedAt() == null)
                .filter(value -> totpService.isEnabled(value.getId()))
                .orElseThrow(() -> new BusinessException("TOTP_UNAVAILABLE", HttpStatus.BAD_REQUEST,
                        "Authenticator sign-in is not available for this account"));
        return new TotpChallengeResponse(totpService.createLoginChallenge(user.getId()));
    }

    @Transactional
    public AuthTokenResponse verifyTotp(TotpLoginRequest req, String deviceInfo) {
        return issueTokensAfterStrongAuth(
                totpService.verifyLogin(req.challengeToken(), req.code()), deviceInfo, "TOTP");
    }

    @Transactional
    public AuthTokenResponse issueTokensAfterStrongAuth(Long userId, String deviceInfo, String method) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (user.getStatus() != Enums.UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw new BusinessException("ACCOUNT_NOT_ACTIVE", HttpStatus.FORBIDDEN, "Account is not active");
        }
        return issueTokens(user, deviceInfo, Instant.now(), method);
    }

    @Transactional
    public AuthTokenResponse refresh(RefreshRequest req, String deviceInfo) {
        String hash = sha256(req.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash)
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH", HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("INVALID_REFRESH", HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        // rotate
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", stored.getUserId()));
        // SECURITY: a suspended or deleted account cannot refresh into new access tokens.
        if (user.getStatus() != Enums.UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw new BusinessException("ACCOUNT_NOT_ACTIVE", HttpStatus.FORBIDDEN, "Account is not active");
        }
        Instant authenticatedAt = stored.getAuthenticatedAt() != null
                ? stored.getAuthenticatedAt() : Instant.EPOCH;
        String authMethod = stored.getAuthMethod() != null ? stored.getAuthMethod() : "LEGACY";
        return issueTokens(user, deviceInfo, authenticatedAt, authMethod);
    }

    @Transactional
    public void logout(RefreshRequest req) {
        String hash = sha256(req.refreshToken());
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash).ifPresent(rt -> {
            rt.setRevokedAt(Instant.now());
            refreshTokenRepository.save(rt);
        });
    }

    /** Issues access + refresh tokens for an already-resolved user (used by dev login). */
    @Transactional
    public AuthTokenResponse issueTokensForUser(User user) {
        return issueTokens(user, "dev-login", Instant.now(), "DEV");
    }

    @Transactional
    public AuthTokenResponse issueDemoTokensForUser(User user) {
        Set<RoleName> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        if (user.getStatus() != Enums.UserStatus.ACTIVE || user.getDeletedAt() != null
                || roles.contains(RoleName.ADMIN)
                || !(roles.equals(Set.of(RoleName.RETAILER)) || roles.equals(Set.of(RoleName.SUPPLIER)))) {
            throw new BusinessException("DEMO_ACCOUNT_INVALID", HttpStatus.FORBIDDEN,
                    "Demo account is unavailable");
        }
        return issueTokens(user, "public-demo", Instant.now(), "DEMO");
    }

    @Transactional
    public AuthTokenResponse issueOAuthTokens(Long userId, String deviceInfo, String provider) {
        return issueTokensAfterStrongAuth(userId, deviceInfo, "OAUTH_" + provider.toUpperCase(java.util.Locale.ROOT));
    }

    private AuthTokenResponse issueTokens(User user, String deviceInfo,
                                          Instant authenticatedAt, String authMethod) {
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name()).collect(Collectors.toSet());
        String access = tokenProvider.generateAccessToken(
                user.getId(), user.getUuid(), user.getPhone(), roles, authenticatedAt, authMethod);

        String rawRefresh = generateRefreshToken();
        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setTokenHash(sha256(rawRefresh));
        rt.setDeviceInfo(deviceInfo);
        rt.setAuthenticatedAt(authenticatedAt);
        rt.setAuthMethod(authMethod);
        rt.setExpiresAt(Instant.now().plus(props.getSecurity().getJwt().getRefreshTokenTtlDays(), ChronoUnit.DAYS));
        refreshTokenRepository.save(rt);

        long expiresIn = props.getSecurity().getJwt().getAccessTokenTtlMinutes() * 60;
        return AuthTokenResponse.authenticated(access, rawRefresh, expiresIn, UserResponse.from(user));
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256(String value) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

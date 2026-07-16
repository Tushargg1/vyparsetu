package com.vyaparsetu.auth.service;

import com.vyaparsetu.auth.dto.*;
import com.vyaparsetu.auth.entity.OtpToken;
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
    private final OtpService otpService;
    private final TotpService totpService;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder encoder;
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       RetailerRepository retailerRepository, SupplierRepository supplierRepository,
                       RefreshTokenRepository refreshTokenRepository, OtpService otpService,
                       TotpService totpService, JwtTokenProvider tokenProvider,
                       PasswordEncoder encoder, AppProperties props) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.retailerRepository = retailerRepository;
        this.supplierRepository = supplierRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.otpService = otpService;
        this.totpService = totpService;
        this.tokenProvider = tokenProvider;
        this.encoder = encoder;
        this.props = props;
    }

    @Transactional
    public void register(RegisterRequest req) {
        // SECURITY: never allow privileged roles to be created via public self-registration.
        if (req.role() == null || req.role() == RoleName.ADMIN) {
            throw new BusinessException("ROLE_NOT_ALLOWED", HttpStatus.FORBIDDEN,
                    "This role cannot be self-registered");
        }
        if (userRepository.existsByPhone(req.phone())) {
            throw new BusinessException("PHONE_TAKEN", HttpStatus.CONFLICT, "Phone already registered");
        }
        if (req.email() != null && userRepository.existsByEmail(req.email())) {
            throw new BusinessException("EMAIL_TAKEN", HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setName(req.name());
        user.setPhone(req.phone());
        user.setEmail(req.email());
        user.setStatus(Enums.UserStatus.PENDING);
        if (req.preferredLanguage() != null) {
            user.setPreferredLanguage(req.preferredLanguage());
        }
        Role role = roleRepository.findByName(req.role())
                .orElseThrow(() -> new ResourceNotFoundException("Role", req.role()));
        user.setRoles(Set.of(role));
        user = userRepository.save(user);

        createRoleProfile(user.getId(), req);

        // send registration OTP to phone
        otpService.generateAndSend(req.phone(), Enums.OtpChannel.SMS, Enums.OtpPurpose.REGISTER, user.getId());
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
    public void sendOtp(SendOtpRequest req) {
        Long userId = null;
        if (req.purpose() == Enums.OtpPurpose.LOGIN) {
            // SECURITY: do not reveal whether an account exists (anti-enumeration).
            // Silently no-op for unknown identifiers.
            var existing = userRepository.findByPhone(req.identifier())
                    .or(() -> userRepository.findByEmail(req.identifier()));
            if (existing.isEmpty()) {
                return;
            }
            userId = existing.get().getId();
        }
        otpService.generateAndSend(req.identifier(), req.channel(), req.purpose(), userId);
    }

    @Transactional
    public AuthTokenResponse verifyOtp(VerifyOtpRequest req, String deviceInfo) {
        OtpToken token = otpService.verify(req.identifier(), req.code(), req.purpose());

        User user = findByIdentifier(req.identifier());
        if (req.purpose() == Enums.OtpPurpose.REGISTER) {
            user.setStatus(Enums.UserStatus.ACTIVE);
        } else if (user.getStatus() != Enums.UserStatus.ACTIVE) {
            // SECURITY: suspended / not-yet-activated accounts cannot obtain tokens.
            throw new BusinessException("ACCOUNT_NOT_ACTIVE", HttpStatus.FORBIDDEN,
                    "Account is not active");
        }
        if (token.getChannel() == Enums.OtpChannel.SMS) {
            user.setPhoneVerified(true);
        } else {
            user.setEmailVerified(true);
        }
        userRepository.save(user);
        return issueTokens(user, deviceInfo, Instant.now(), "OTP");
    }

    @Transactional
    public TotpChallengeResponse startTotpLogin(TotpOptionsRequest req) {
        User user = userRepository.findByPhone(req.identifier())
                .or(() -> userRepository.findByEmail(req.identifier()))
                .filter(value -> value.getStatus() == Enums.UserStatus.ACTIVE)
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
        if (user.getStatus() != Enums.UserStatus.ACTIVE) {
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
        // SECURITY: a suspended account cannot refresh into new access tokens.
        if (user.getStatus() != Enums.UserStatus.ACTIVE) {
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

    private User findByIdentifier(String identifier) {
        return userRepository.findByPhone(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("User", identifier));
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

package com.vyaparsetu.auth.service;

import com.vyaparsetu.auth.dto.TotpSetupResponse;
import com.vyaparsetu.auth.entity.AuthenticationChallenge;
import com.vyaparsetu.auth.entity.TotpCredential;
import com.vyaparsetu.auth.repository.AuthenticationChallengeRepository;
import com.vyaparsetu.auth.repository.TotpCredentialRepository;
import com.vyaparsetu.common.config.AppProperties;
import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class TotpService {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private final TotpCredentialRepository credentials;
    private final AuthenticationChallengeRepository challenges;
    private final SecretEncryptionService encryption;
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();

    public TotpService(TotpCredentialRepository credentials,
                       AuthenticationChallengeRepository challenges,
                       SecretEncryptionService encryption, AppProperties props) {
        this.credentials = credentials;
        this.challenges = challenges;
        this.encryption = encryption;
        this.props = props;
    }

    public boolean isEnabled(Long userId) {
        return credentials.existsByUserIdAndEnabledTrue(userId);
    }
    @Transactional
    public TotpSetupResponse setup(User user) {
        if (isEnabled(user.getId())) {
            throw new BusinessException("TOTP_ALREADY_ENABLED", HttpStatus.CONFLICT,
                    "Disable the current authenticator before replacing it");
        }
        byte[] secretBytes = new byte[20];
        random.nextBytes(secretBytes);
        String secret = base32Encode(secretBytes);
        TotpCredential credential = new TotpCredential();
        credential.setUserId(user.getId());
        credential.setEncryptedSecret(encryption.encrypt(secret));
        credential.setEnabled(false);
        credential.setLastUsedStep(null);
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credentials.save(credential);

        String issuer = props.getSecurity().getTotp().getIssuer();
        String account = user.getPhone() != null ? user.getPhone() : user.getUuid();
        String label = url(issuer + ":" + account);
        String uri = "otpauth://totp/" + label + "?secret=" + secret
                + "&issuer=" + url(issuer) + "&algorithm=SHA1&digits=6&period=30";
        return new TotpSetupResponse(secret, uri);
    }

    @Transactional
    public void confirm(Long userId, String code) {
        TotpCredential credential = getCredential(userId);
        long step = matchingStep(credential, code, false);
        if (step < 0) {
            throw invalidCode();
        }
        credential.setEnabled(true);
        credential.setVerifiedAt(Instant.now());
        credential.setLastUsedStep(step);
        credentials.save(credential);
    }

    @Transactional
    public void disable(Long userId, String code) {
        TotpCredential credential = getEnabledCredential(userId);
        if (matchingStep(credential, code, false) < 0) {
            throw invalidCode();
        }
        credentials.delete(credential);
    }

    @Transactional
    public String createLoginChallenge(Long userId) {
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        AuthenticationChallenge challenge = new AuthenticationChallenge();
        challenge.setTokenHash(hash(token));
        challenge.setUserId(userId);
        challenge.setChallengeType(AuthenticationChallenge.Type.TOTP_LOGIN);
        challenge.setExpiresAt(Instant.now().plus(
                props.getSecurity().getTotp().getChallengeTtlMinutes(), ChronoUnit.MINUTES));
        challenges.save(challenge);
        return token;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BusinessException.class)
    public Long verifyLogin(String token, String code) {
        AuthenticationChallenge challenge = challenges
                .findActiveForUpdate(hash(token), AuthenticationChallenge.Type.TOTP_LOGIN)
                .orElseThrow(() -> new BusinessException("INVALID_MFA_CHALLENGE",
                        HttpStatus.UNAUTHORIZED, "Authentication challenge is invalid or expired"));
        if (challenge.getExpiresAt().isBefore(Instant.now()) || challenge.getAttempts() >= 5) {
            throw new BusinessException("INVALID_MFA_CHALLENGE", HttpStatus.UNAUTHORIZED,
                    "Authentication challenge is invalid or expired");
        }

        TotpCredential credential = getEnabledCredentialForUpdate(challenge.getUserId());
        if (credential.getLockedUntil() != null && credential.getLockedUntil().isAfter(Instant.now())) {
            throw new BusinessException("TOTP_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts. Try again in a few minutes");
        }
        if (credential.getLockedUntil() != null) {
            credential.setLockedUntil(null);
            credential.setFailedAttempts(0);
        }
        long step = matchingStep(credential, code, true);
        if (step < 0) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            challenges.save(challenge);
            int failures = credential.getFailedAttempts() + 1;
            credential.setFailedAttempts(failures);
            if (failures >= 5) {
                credential.setLockedUntil(Instant.now().plus(5, ChronoUnit.MINUTES));
            }
            credentials.save(credential);
            if (failures >= 5) {
                throw new BusinessException("TOTP_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                        "Too many attempts. Try again in a few minutes");
            }
            throw invalidCode();
        }
        credential.setLastUsedStep(step);
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credentials.save(credential);
        challenge.setConsumedAt(Instant.now());
        challenges.save(challenge);
        return challenge.getUserId();
    }

    private TotpCredential getCredential(Long userId) {
        return credentials.findById(userId).orElseThrow(() ->
                new BusinessException("TOTP_NOT_CONFIGURED", HttpStatus.NOT_FOUND,
                        "Authenticator app is not configured"));
    }

    private TotpCredential getEnabledCredentialForUpdate(Long userId) {
        TotpCredential credential = credentials.findByUserIdForUpdate(userId).orElseThrow(() ->
                new BusinessException("TOTP_NOT_CONFIGURED", HttpStatus.NOT_FOUND,
                        "Authenticator app is not configured"));
        if (!credential.isEnabled()) {
            throw new BusinessException("TOTP_NOT_CONFIGURED", HttpStatus.BAD_REQUEST,
                    "Authenticator app is not enabled");
        }
        return credential;
    }

    private TotpCredential getEnabledCredential(Long userId) {
        TotpCredential credential = getCredential(userId);
        if (!credential.isEnabled()) {
            throw new BusinessException("TOTP_NOT_CONFIGURED", HttpStatus.BAD_REQUEST,
                    "Authenticator app is not enabled");
        }
        return credential;
    }

    private long matchingStep(TotpCredential credential, String code, boolean rejectReplay) {
        String secret = encryption.decrypt(credential.getEncryptedSecret());
        long current = Instant.now().getEpochSecond() / 30;
        for (long step = current - 1; step <= current + 1; step++) {
            if ((!rejectReplay || credential.getLastUsedStep() == null || step > credential.getLastUsedStep())
                    && MessageDigest.isEqual(generate(secret, step).getBytes(StandardCharsets.US_ASCII),
                    code.getBytes(StandardCharsets.US_ASCII))) {
                return step;
            }
        }
        return -1;
    }
    private static String generate(String base32Secret, long step) {
        try {
            byte[] counter = new byte[8];
            for (int i = 7; i >= 0; i--) {
                counter[i] = (byte) step;
                step >>>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(base32Decode(base32Secret), "HmacSHA1"));
            byte[] digest = mac.doFinal(counter);
            int offset = digest[digest.length - 1] & 0x0f;
            int binary = ((digest[offset] & 0x7f) << 24)
                    | ((digest[offset + 1] & 0xff) << 16)
                    | ((digest[offset + 2] & 0xff) << 8)
                    | (digest[offset + 3] & 0xff);
            return String.format("%06d", binary % 1_000_000);
        } catch (Exception e) {
            throw new IllegalStateException("Could not verify authenticator code", e);
        }
    }

    private static String base32Encode(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte value : data) {
            buffer = (buffer << 8) | (value & 0xff);
            bits += 8;
            while (bits >= 5) {
                out.append(ALPHABET.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) out.append(ALPHABET.charAt((buffer << (5 - bits)) & 31));
        return out.toString();
    }

    private static byte[] base32Decode(String value) {
        byte[] result = new byte[value.length() * 5 / 8];
        int buffer = 0;
        int bits = 0;
        int index = 0;
        for (char c : value.toUpperCase().toCharArray()) {
            int digit = ALPHABET.indexOf(c);
            if (digit < 0) continue;
            buffer = (buffer << 5) | digit;
            bits += 5;
            if (bits >= 8) {
                result[index++] = (byte) (buffer >> (bits - 8));
                bits -= 8;
            }
        }
        return result;
    }
    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static BusinessException invalidCode() {
        return new BusinessException("INVALID_TOTP", HttpStatus.UNAUTHORIZED,
                "Invalid authenticator code");
    }
}

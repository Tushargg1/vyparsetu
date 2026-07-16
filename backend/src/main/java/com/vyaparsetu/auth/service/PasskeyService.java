package com.vyaparsetu.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.vyaparsetu.auth.dto.*;
import com.vyaparsetu.auth.entity.PasskeyCredential;
import com.vyaparsetu.auth.entity.WebAuthnCeremony;
import com.vyaparsetu.auth.repository.PasskeyCredentialRepository;
import com.vyaparsetu.auth.repository.WebAuthnCeremonyRepository;
import com.vyaparsetu.common.config.AppProperties;
import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.user.entity.User;
import com.vyaparsetu.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PasskeyService {
    private final PasskeyCredentialRepository credentials;
    private final WebAuthnCeremonyRepository ceremonies;
    private final UserRepository users;
    private final DatabaseCredentialRepository credentialLookup;
    private final ObjectMapper mapper;
    private final AppProperties props;
    private final RelyingParty relyingParty;

    public PasskeyService(PasskeyCredentialRepository credentials,
                          WebAuthnCeremonyRepository ceremonies, UserRepository users,
                          DatabaseCredentialRepository credentialLookup,
                          ObjectMapper mapper, AppProperties props) {
        this.credentials = credentials;
        this.ceremonies = ceremonies;
        this.users = users;
        this.credentialLookup = credentialLookup;
        this.mapper = mapper;
        this.props = props;
        AppProperties.WebAuthn config = props.getSecurity().getWebauthn();
        RelyingPartyIdentity identity = RelyingPartyIdentity.builder()
                .id(config.getRpId()).name(config.getRpName()).build();
        this.relyingParty = RelyingParty.builder()
                .identity(identity)
                .credentialRepository(credentialLookup)
                .origins(Set.copyOf(config.getOrigins()))
                .allowUntrustedAttestation(true)
                .validateSignatureCounter(true)
                .build();
    }

    @Transactional
    public PasskeyOptionsResponse startRegistration(Long userId) {
        User user = activeUser(userId);
        UserIdentity identity = UserIdentity.builder()
                .name(user.getUuid())
                .displayName(user.getName())
                .id(new ByteArray(credentialLookup.userHandle(user)))
                .build();
        PublicKeyCredentialCreationOptions request = relyingParty.startRegistration(
                StartRegistrationOptions.builder().user(identity).timeout(120_000L).build());
        return saveRegistrationOptions(userId, request);
    }

    @Transactional
    public PasskeySummary finishRegistration(Long userId, PasskeyFinishRequest input) {
        try {
            WebAuthnCeremony ceremony = requireCeremony(input.ceremonyId(),
                    WebAuthnCeremony.Type.REGISTER, userId);
            PublicKeyCredentialCreationOptions request =
                    PublicKeyCredentialCreationOptions.fromJson(ceremony.getRequestJson());
            PublicKeyCredential<AuthenticatorAttestationResponse,
                    ClientRegistrationExtensionOutputs> response =
                    PublicKeyCredential.parseRegistrationResponseJson(input.credential().toString());
            RegistrationResult result = relyingParty.finishRegistration(
                    FinishRegistrationOptions.builder().request(request).response(response).build());

            PasskeyCredential value = new PasskeyCredential();
            value.setUserId(userId);
            value.setCredentialId(result.getKeyId().getId().getBytes());
            value.setUserHandle(credentialLookup.userHandle(activeUser(userId)));
            value.setPublicKeyCose(result.getPublicKeyCose().getBytes());
            value.setSignatureCount(result.getSignatureCount());
            value.setDisplayName(safeName(input.name()));
            value = credentials.save(value);
            ceremony.setConsumedAt(Instant.now());
            ceremonies.save(ceremony);
            return summary(value);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw invalidPasskey(e);
        }
    }
    @Transactional
    public PasskeyOptionsResponse startAuthentication(String identifier) {
        User user = users.findByPhone(identifier).or(() -> users.findByEmail(identifier))
                .orElseThrow(() -> unavailable());
        if (user.getStatus() != Enums.UserStatus.ACTIVE || credentials.countByUserId(user.getId()) == 0) {
            throw unavailable();
        }
        AssertionRequest request = relyingParty.startAssertion(
                StartAssertionOptions.builder().username(user.getUuid())
                        .userVerification(UserVerificationRequirement.REQUIRED)
                        .timeout(120_000L).build());
        return saveAuthenticationOptions(user.getId(), request);
    }

    @Transactional
    public Long finishAuthentication(PasskeyFinishRequest input) {
        try {
            WebAuthnCeremony ceremony = requireCeremony(input.ceremonyId(),
                    WebAuthnCeremony.Type.AUTHENTICATE, null);
            User user = activeUser(ceremony.getUserId());
            AssertionRequest request = AssertionRequest.fromJson(ceremony.getRequestJson());
            PublicKeyCredential<AuthenticatorAssertionResponse,
                    ClientAssertionExtensionOutputs> response =
                    PublicKeyCredential.parseAssertionResponseJson(input.credential().toString());
            AssertionResult result = relyingParty.finishAssertion(
                    FinishAssertionOptions.builder().request(request).response(response).build());
            if (!result.isSuccess() || !user.getUuid().equals(result.getUsername()) || !result.isUserVerified()) {
                throw invalidPasskey(null);
            }
            PasskeyCredential value = credentials
                    .findForUpdate(result.getCredentialId().getBytes(), result.getUserHandle().getBytes())
                    .orElseThrow(() -> invalidPasskey(null));
            if (result.getSignatureCount() > 0 && value.getSignatureCount() > 0
                    && result.getSignatureCount() <= value.getSignatureCount()) {
                throw invalidPasskey(null);
            }
            value.setSignatureCount(result.getSignatureCount());
            value.setLastUsedAt(Instant.now());
            credentials.save(value);
            ceremony.setConsumedAt(Instant.now());
            ceremonies.save(ceremony);
            return user.getId();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw invalidPasskey(e);
        }
    }

    public SecurityMethodsResponse status(Long userId, boolean totpEnabled) {
        List<PasskeySummary> passkeys = credentials.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(PasskeyService::summary).toList();
        return new SecurityMethodsResponse(totpEnabled, passkeys);
    }
    @Transactional
    public void delete(Long userId, Long passkeyId) {
        PasskeyCredential value = credentials.findById(passkeyId)
                .filter(item -> item.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Passkey", passkeyId));
        credentials.delete(value);
    }

    private PasskeyOptionsResponse saveRegistrationOptions(
            Long userId, PublicKeyCredentialCreationOptions request) {
        try {
            return saveOptions(userId, WebAuthnCeremony.Type.REGISTER,
                    json(request.toJson()), json(request.toCredentialsCreateJson()));
        } catch (Exception e) {
            throw new IllegalStateException("Could not create passkey options", e);
        }
    }

    private PasskeyOptionsResponse saveAuthenticationOptions(Long userId, AssertionRequest request) {
        try {
            return saveOptions(userId, WebAuthnCeremony.Type.AUTHENTICATE,
                    json(request.toJson()), json(request.toCredentialsGetJson()));
        } catch (Exception e) {
            throw new IllegalStateException("Could not create passkey options", e);
        }
    }

    private PasskeyOptionsResponse saveOptions(Long userId, WebAuthnCeremony.Type type,
                                               JsonNode requestJson, JsonNode browserOptions) {
        WebAuthnCeremony ceremony = new WebAuthnCeremony();
        ceremony.setId(UUID.randomUUID().toString());
        ceremony.setUserId(userId);
        ceremony.setCeremonyType(type);
        ceremony.setRequestJson(requestJson.toString());
        ceremony.setExpiresAt(Instant.now().plus(
                props.getSecurity().getWebauthn().getCeremonyTtlMinutes(), ChronoUnit.MINUTES));
        ceremonies.save(ceremony);
        return new PasskeyOptionsResponse(ceremony.getId(), browserOptions);
    }

    private WebAuthnCeremony requireCeremony(String id, WebAuthnCeremony.Type type, Long userId) {
        WebAuthnCeremony value = ceremonies.findActiveForUpdate(id, type)
                .orElseThrow(() -> invalidPasskey(null));
        if (value.getExpiresAt().isBefore(Instant.now())
                || (userId != null && !value.getUserId().equals(userId))) {
            throw invalidPasskey(null);
        }
        return value;
    }

    private User activeUser(Long id) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (user.getStatus() != Enums.UserStatus.ACTIVE) {
            throw new BusinessException("ACCOUNT_NOT_ACTIVE", HttpStatus.FORBIDDEN, "Account is not active");
        }
        return user;
    }

    private JsonNode json(String value) {
        try {
            return mapper.readTree(value);
        } catch (Exception e) {
            throw new IllegalStateException("Could not encode passkey options", e);
        }
    }
    private static PasskeySummary summary(PasskeyCredential value) {
        return new PasskeySummary(value.getId(), value.getDisplayName(),
                value.getCreatedAt(), value.getLastUsedAt());
    }

    private static String safeName(String name) {
        String value = name == null || name.isBlank() ? "My passkey" : name.trim();
        return value.substring(0, Math.min(value.length(), 100));
    }

    private static BusinessException unavailable() {
        return new BusinessException("PASSKEY_UNAVAILABLE", HttpStatus.BAD_REQUEST,
                "No passkey is available for this account");
    }

    private static BusinessException invalidPasskey(Exception cause) {
        return new BusinessException("INVALID_PASSKEY", HttpStatus.UNAUTHORIZED,
                "Passkey verification failed or expired");
    }
}

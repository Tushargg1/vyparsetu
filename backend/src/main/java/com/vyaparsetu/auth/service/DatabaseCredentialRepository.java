package com.vyaparsetu.auth.service;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.vyaparsetu.auth.entity.PasskeyCredential;
import com.vyaparsetu.auth.repository.PasskeyCredentialRepository;
import com.vyaparsetu.user.entity.User;
import com.vyaparsetu.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DatabaseCredentialRepository implements CredentialRepository {
    private final PasskeyCredentialRepository credentials;
    private final UserRepository users;

    public DatabaseCredentialRepository(PasskeyCredentialRepository credentials, UserRepository users) {
        this.credentials = credentials;
        this.users = users;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return users.findByUuid(username).stream()
                .flatMap(user -> credentials.findByUserIdOrderByCreatedAtDesc(user.getId()).stream())
                .map(item -> PublicKeyCredentialDescriptor.builder()
                        .id(new ByteArray(item.getCredentialId())).build())
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return users.findByUuid(username).map(user -> new ByteArray(userHandle(user)));
    }
    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return credentials.findByUserHandle(userHandle.getBytes()).stream().findFirst()
                .flatMap(item -> users.findById(item.getUserId()))
                .map(User::getUuid);
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return credentials.findByCredentialIdAndUserHandle(credentialId.getBytes(), userHandle.getBytes())
                .map(this::toRegistered);
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return credentials.findByCredentialId(credentialId.getBytes()).stream()
                .map(this::toRegistered).collect(Collectors.toSet());
    }

    public byte[] userHandle(User user) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(user.getUuid().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private RegisteredCredential toRegistered(PasskeyCredential value) {
        return RegisteredCredential.builder()
                .credentialId(new ByteArray(value.getCredentialId()))
                .userHandle(new ByteArray(value.getUserHandle()))
                .publicKeyCose(new ByteArray(value.getPublicKeyCose()))
                .signatureCount(value.getSignatureCount())
                .build();
    }
}

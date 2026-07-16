package com.vyaparsetu.auth.service;

import com.vyaparsetu.common.config.AppProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class SecretEncryptionService {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SecretEncryptionService(AppProperties props) {
        String configured = props.getSecurity().getTotp().getEncryptionKey();
        String source = configured == null || configured.isBlank()
                ? props.getSecurity().getJwt().getSecret() : configured;
        if (source == null || source.length() < 32) {
            throw new IllegalStateException("TOTP encryption requires a secret of at least 32 characters");
        }
        this.key = new SecretKeySpec(sha256("vyaparmantra:totp:" + source), "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return encode(iv) + "." + encode(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not protect authenticator secret", e);
        }
    }
    public String decrypt(String protectedValue) {
        try {
            String[] parts = protectedValue.split("\\.", 2);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, decode(parts[0])));
            return new String(cipher.doFinal(decode(parts[1])), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read authenticator secret", e);
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}

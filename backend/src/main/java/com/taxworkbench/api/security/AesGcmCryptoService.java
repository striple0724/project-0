package com.taxworkbench.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmCryptoService {
    private static final String ALG = "AES";
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int IV_LEN = 12;
    private static final int TAG_LEN = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] key;

    public AesGcmCryptoService(@Value("${app.security.crypto-key-base64:}") String keyBase64) {
        if (keyBase64 == null || keyBase64.isBlank()) {
            this.key = null;
            return;
        }
        this.key = Base64.getDecoder().decode(keyBase64);
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        if (key == null) {
            throw new IllegalStateException("CRYPTO_KEY_NOT_CONFIGURED");
        }
        try {
            byte[] iv = new byte[IV_LEN];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ALG), new GCMParameterSpec(TAG_LEN, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("CRYPTO_ENCRYPT_FAILED", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        if (key == null) {
            throw new IllegalStateException("CRYPTO_KEY_NOT_CONFIGURED");
        }
        try {
            byte[] raw = Base64.getDecoder().decode(cipherText);
            byte[] iv = new byte[IV_LEN];
            byte[] encrypted = new byte[raw.length - IV_LEN];
            System.arraycopy(raw, 0, iv, 0, IV_LEN);
            System.arraycopy(raw, IV_LEN, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, ALG), new GCMParameterSpec(TAG_LEN, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("CRYPTO_DECRYPT_FAILED", e);
        }
    }
}

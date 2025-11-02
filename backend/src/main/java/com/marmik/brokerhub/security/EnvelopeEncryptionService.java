package com.marmik.brokerhub.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Envelope encryption helper.
 *
 * - Generates per-row DEKs (32 bytes).
 * - Encrypts data with AES-256-GCM (12-byte IV, 128-bit tag).
 * - Wraps DEK with master key using AES-256-GCM (12-byte IV) and returns
 * wrapper = iv || ciphertext.
 *
 * Notes:
 * - The master key is read from app.security.master-key-base64 and MUST be 32
 * raw bytes when base64-decoded.
 * - This class avoids logging secret material. Callers must zero sensitive
 * byte[] after use.
 */
@Component
public class EnvelopeEncryptionService {

    private static final int DEK_BYTES = 32; // 256-bit DEK
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey masterKey;

    public EnvelopeEncryptionService(@Value("${app.security.master-key-base64}") String masterKeyBase64) {
        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            throw new IllegalArgumentException("app.security.master-key-base64 must be configured");
        }
        byte[] raw = java.util.Base64.getDecoder().decode(masterKeyBase64);
        if (raw.length != 32) {
            throw new IllegalArgumentException("master key must be 32 bytes (base64 decode result)");
        }
        this.masterKey = new SecretKeySpec(raw, "AES");

        // zero raw copy for safety
        Arrays.fill(raw, (byte) 0);
    }

    /**
     * Generate a fresh random DEK (32 bytes).
     * Caller must zero the returned array after use.
     */
    public byte[] generateDek() {
        byte[] dek = new byte[DEK_BYTES];
        secureRandom.nextBytes(dek);
        return dek;
    }

    /**
     * Encrypt plaintext with given DEK using AES-256-GCM.
     * Returns an array-wrapper: { cipherBytes, iv } in an object.
     * Caller must zero plain and dek after use.
     */
    public EncryptionResult encryptWithDek(byte[] dek, byte[] plaintext) throws Exception {
        byte[] iv = new byte[GCM_IV_BYTES];
        secureRandom.nextBytes(iv);

        SecretKey dekKey = new SecretKeySpec(dek, "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, dekKey, spec);
        byte[] cipherText = cipher.doFinal(plaintext);

        // do not zero plaintext here; caller manages that
        return new EncryptionResult(cipherText, iv);
    }

    /**
     * Decrypt ciphertext with given DEK and IV.
     * Caller must zero the returned plaintext and supplied dek when done.
     */
    public byte[] decryptWithDek(byte[] dek, byte[] iv, byte[] cipherText) throws Exception {
        SecretKey dekKey = new SecretKeySpec(dek, "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, dekKey, spec);
        return cipher.doFinal(cipherText);
    }

    /**
     * Wrap (encrypt) the per-row DEK using the master key.
     * We use AES-GCM with a random 12-byte IV. The returned byte[] is IV ||
     * ciphertext.
     *
     * Caller must zero dek after use.
     */
    public byte[] wrapDek(byte[] dek) throws Exception {
        byte[] iv = new byte[GCM_IV_BYTES];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);
        byte[] wrapped = cipher.doFinal(dek);

        // combine iv || wrapped
        byte[] out = new byte[iv.length + wrapped.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(wrapped, 0, out, iv.length, wrapped.length);

        // zero sensitive local arrays
        Arrays.fill(iv, (byte) 0);
        Arrays.fill(wrapped, (byte) 0);

        return out;
    }

    /**
     * Unwrap DEK previously wrapped by wrapDek (iv || ciphertext).
     * Returns the raw DEK bytes; caller must zero after use.
     */
    public byte[] unwrapDek(byte[] wrappedIvAndCiphertext) throws Exception {
        if (wrappedIvAndCiphertext == null || wrappedIvAndCiphertext.length <= GCM_IV_BYTES) {
            throw new IllegalArgumentException("invalid wrapped dek format");
        }
        byte[] iv = Arrays.copyOfRange(wrappedIvAndCiphertext, 0, GCM_IV_BYTES);
        byte[] cipherBytes = Arrays.copyOfRange(wrappedIvAndCiphertext, GCM_IV_BYTES, wrappedIvAndCiphertext.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, spec);
        byte[] dek = cipher.doFinal(cipherBytes);

        // zero temp buffers
        Arrays.fill(iv, (byte) 0);
        Arrays.fill(cipherBytes, (byte) 0);

        return dek;
    }

    /**
     * Simple holder for encryption result.
     */
    public static final class EncryptionResult {
        private final byte[] cipherText;
        private final byte[] iv;

        public EncryptionResult(byte[] cipherText, byte[] iv) {
            this.cipherText = cipherText;
            this.iv = iv;
        }

        public byte[] getCipherText() {
            return cipherText;
        }

        public byte[] getIv() {
            return iv;
        }
    }
}

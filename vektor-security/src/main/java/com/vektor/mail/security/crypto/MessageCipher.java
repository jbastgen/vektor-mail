package com.vektor.mail.security.crypto;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * AES-256-GCM encryption for message content.
 *
 * Each message gets its own Data Encryption Key (DEK). The DEK is wrapped
 * (encrypted) with the server Master Key before being stored in the DB.
 */
@Component
public class MessageCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12;        // bytes

    private final SecureRandom random = new SecureRandom();

    /** Generate a fresh 256-bit AES DEK. */
    public byte[] generateDek() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256, random);
        return kg.generateKey().getEncoded();
    }

    /**
     * Encrypt plaintext with the given DEK.
     * Output format: [12-byte IV][ciphertext+tag]
     */
    public byte[] encrypt(byte[] plaintext, byte[] dek) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(dek, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] ciphertext = cipher.doFinal(plaintext);

        byte[] result = new byte[IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, IV_LENGTH);
        System.arraycopy(ciphertext, 0, result, IV_LENGTH, ciphertext.length);
        return result;
    }

    /**
     * Decrypt ciphertext that was produced by {@link #encrypt}.
     */
    public byte[] decrypt(byte[] ciphertext, byte[] dek) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(ciphertext, 0, iv, 0, IV_LENGTH);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(dek, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(ciphertext, IV_LENGTH, ciphertext.length - IV_LENGTH);
    }

    /**
     * Wrap (encrypt) a DEK with the master key.
     * Uses the same AES-GCM approach — master key protects the DEK.
     */
    public byte[] wrapDek(byte[] dek, byte[] masterKey) throws Exception {
        return encrypt(dek, masterKey);
    }

    /**
     * Unwrap (decrypt) a DEK that was wrapped with the master key.
     */
    public byte[] unwrapDek(byte[] wrappedDek, byte[] masterKey) throws Exception {
        return decrypt(wrappedDek, masterKey);
    }
}

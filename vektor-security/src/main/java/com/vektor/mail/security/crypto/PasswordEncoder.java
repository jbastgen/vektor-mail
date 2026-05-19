package com.vektor.mail.security.crypto;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Argon2id password hashing.
 */
@Component
public class PasswordEncoder {

    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;
    private static final int ITERATIONS = 3;
    private static final int MEMORY_KB = 65536;
    private static final int PARALLELISM = 4;

    private final SecureRandom random = new SecureRandom();

    public String hash(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);

        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withIterations(ITERATIONS)
                .withMemoryAsKB(MEMORY_KB)
                .withParallelism(PARALLELISM)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);
        byte[] hash = new byte[HASH_LENGTH];
        generator.generateBytes(password.toCharArray(), hash);

        // Format: $argon2id$v=19$m=65536,t=3,p=4$<salt_b64>$<hash_b64>
        return "$argon2id$v=19$m=%d,t=%d,p=%d$%s$%s".formatted(
                MEMORY_KB, ITERATIONS, PARALLELISM,
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash));
    }

    public boolean verify(String password, String encoded) {
        try {
            String[] parts = encoded.split("\\$");
            // parts: ["", "argon2id", "v=19", "m=...,t=...,p=...", saltB64, hashB64]
            String[] paramParts = parts[3].split(",");
            int m = Integer.parseInt(paramParts[0].substring(2));
            int t = Integer.parseInt(paramParts[1].substring(2));
            int p = Integer.parseInt(paramParts[2].substring(2));
            byte[] salt = Base64.getDecoder().decode(parts[4]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[5]);

            Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withSalt(salt)
                    .withIterations(t)
                    .withMemoryAsKB(m)
                    .withParallelism(p)
                    .build();

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(params);
            byte[] actual = new byte[expectedHash.length];
            generator.generateBytes(password.toCharArray(), actual);

            return constantTimeEquals(actual, expectedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }
}

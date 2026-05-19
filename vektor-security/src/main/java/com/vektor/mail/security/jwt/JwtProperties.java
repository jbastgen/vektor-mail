package com.vektor.mail.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vektor.security.jwt")
public record JwtProperties(
        String privateKey,
        String publicKey,
        long accessTokenTtlSeconds,
        long refreshTokenTtlSeconds
) {
    public JwtProperties {
        if (accessTokenTtlSeconds <= 0) accessTokenTtlSeconds = 900L;       // 15 min
        if (refreshTokenTtlSeconds <= 0) refreshTokenTtlSeconds = 2592000L; // 30 days
    }
}

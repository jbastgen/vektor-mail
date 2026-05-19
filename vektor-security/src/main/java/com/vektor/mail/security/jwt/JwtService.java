package com.vektor.mail.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Issues and validates RS256 JWT tokens.
 */
@Service
@EnableConfigurationProperties(JwtProperties.class)
public class JwtService {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TOKEN_ID = "jti";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final JwtProperties props;

    public JwtService(JwtProperties props) throws Exception {
        this.props = props;
        this.privateKey = loadPrivateKey(props.privateKey());
        this.publicKey = loadPublicKey(props.publicKey());
    }

    public String issueAccessToken(String subject, Set<String> roles) {
        return Jwts.builder()
                .subject(subject)
                .claim(CLAIM_ROLES, roles)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(props.accessTokenTtlSeconds())))
                .id(UUID.randomUUID().toString())
                .signWith(privateKey)
                .compact();
    }

    public String issueRefreshToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(props.refreshTokenTtlSeconds())))
                .id(UUID.randomUUID().toString())
                .claim("type", "refresh")
                .signWith(privateKey)
                .compact();
    }

    public Claims validate(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            validate(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private PrivateKey loadPrivateKey(String pem) throws Exception {
        try (PemReader reader = new PemReader(new StringReader(pem))) {
            PemObject obj = reader.readPemObject();
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(obj.getContent());
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        }
    }

    private PublicKey loadPublicKey(String pem) throws Exception {
        try (PemReader reader = new PemReader(new StringReader(pem))) {
            PemObject obj = reader.readPemObject();
            X509EncodedKeySpec spec = new X509EncodedKeySpec(obj.getContent());
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        }
    }
}

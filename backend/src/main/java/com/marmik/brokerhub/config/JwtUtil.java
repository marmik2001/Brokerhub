package com.marmik.brokerhub.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtUtil {
    private final Key key;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("jwt.secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generate a JWT containing:
     * - sub: memberId
     * - claim "accountId"
     * - claim "role"
     */
    public String generateToken(String memberId, String accountId, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(memberId)
                .claim("accountId", accountId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parse claims from a token. Throws JwtException on invalid token.
     */
    public Jws<Claims> parseClaimsJws(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    /**
     * Validate token signature & expiry. Returns true if valid.
     */
    public boolean validateToken(String token) {
        try {
            parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /** Convenience getters that return Optional.empty() if token invalid. */
    public Optional<String> getMemberId(String token) {
        try {
            Claims c = parseClaimsJws(token).getBody();
            return Optional.ofNullable(c.getSubject());
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    public Optional<String> getAccountId(String token) {
        try {
            Claims c = parseClaimsJws(token).getBody();
            Object v = c.get("accountId");
            return Optional.ofNullable(v == null ? null : v.toString());
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    public Optional<String> getRole(String token) {
        try {
            Claims c = parseClaimsJws(token).getBody();
            Object v = c.get("role");
            return Optional.ofNullable(v == null ? null : v.toString());
        } catch (JwtException e) {
            return Optional.empty();
        }
    }
}

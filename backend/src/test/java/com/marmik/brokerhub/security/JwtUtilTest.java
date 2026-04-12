package com.marmik.brokerhub.security;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JwtUtil.
 *
 * Covers:
 * - Subject extraction and validation for valid and invalid JWTs.
 * - Signature verification using different secrets.
 * - Constructor guard for insecure secret length.
 *
 * Ensures that token integrity and JWT security constraints are not broken.
 */
class JwtUtilTest {

    private static final String SECRET_1 = "12345678901234567890123456789012";
    private static final String SECRET_2 = "abcdefghijklmnopqrstuvwxyz123456";

    @Test
    void shouldReturnUserIdFromValidToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET_1, 60_000);
        String token = jwtUtil.generateUserToken("user-123");

        Optional<String> userId = jwtUtil.getUserId(token);

        assertTrue(userId.isPresent());
        assertEquals("user-123", userId.get());
    }

    @Test
    void shouldReturnEmptyUserIdFromInvalidToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET_1, 60_000);

        Optional<String> userId = jwtUtil.getUserId("not-a-token");

        assertTrue(userId.isEmpty());
        assertFalse(jwtUtil.validateToken("not-a-token"));
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {
        JwtUtil signer = new JwtUtil(SECRET_1, 60_000);
        JwtUtil validator = new JwtUtil(SECRET_2, 60_000);
        String token = signer.generateUserToken("user-123");

        assertFalse(validator.validateToken(token));
        assertTrue(validator.getUserId(token).isEmpty());
    }

    @Test
    void shouldFailConstructionWhenSecretTooShort() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new JwtUtil("short-secret", 60_000));

        assertEquals("jwt.secret must be at least 32 characters", ex.getMessage());
    }
}

package com.nira.finance.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-key-that-is-long-enough-for-hs256-please",
            60);

    @Test
    void generatesTokenThatRoundTripsUserId() {
        String token = jwtService.generateToken(42L, "nur@example.com");

        assertTrue(jwtService.isValid(token));
        assertEquals(42L, jwtService.extractUserId(token));
    }

    @Test
    void rejectsGarbageTokens() {
        assertFalse(jwtService.isValid("not-a-real-token"));
    }

    @Test
    void rejectsTokenSignedWithADifferentSecret() {
        JwtService other = new JwtService("a-completely-different-secret-key-value", 60);
        String token = other.generateToken(1L, "someone@example.com");

        assertFalse(jwtService.isValid(token));
    }
}

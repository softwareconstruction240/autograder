package edu.byu.cs.util;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    @Test
    void generateToken() {
        String token = JwtUtils.generateToken("testNetId");
        assertNotNull(token);
    }

    @Test
    void generateToken__nullNetId() {
        assertNull(JwtUtils.generateToken(null));
    }

    @Test
    void validateToken() {
        String token = JwtUtils.generateToken("testNetId");
        String netId = JwtUtils.validateToken(token);
        assertEquals("testNetId", netId);
    }

    @Test
    void validateToken__badSignature() {
        String token = generateToken(false);
        assertNull(JwtUtils.validateToken(token));
    }

    @Test
    void validateToken__expired() {
        String token = generateToken(true);
        assertNull(JwtUtils.validateToken(token));
    }

    private String generateToken(boolean expired) {
        Instant expiration = expired
                ? Instant.now().minus(1, ChronoUnit.HOURS)
                : Instant.now().plus(1, ChronoUnit.HOURS);
        return Jwts.builder()
                .subject("testNetId")
                .expiration(Date.from(expiration))
                .compact();
    }
}
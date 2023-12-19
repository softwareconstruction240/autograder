package edu.byu.cs.controller;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class JwtUtils {

    // FIXME: move key to external config
    private static final String SECRET_KEY = "this_will_be_replaced_with_something_that_is_a_better_secret";
    private static final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    public static String generateToken(String netId) {
        return Jwts.builder()
                .subject(netId)
                .expiration(Date.from(Instant.now().plus(4, ChronoUnit.HOURS))) // expires 4 hours from now
                .signWith(key)
                .compact();
    }

    /**
     * Validates a JWT and returns the subject if valid (netID)
     * @param token the JWT to validate
     * @return the subject of the JWT if valid, null otherwise
     */
    public static String validateToken(String token) {

        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException e) {
            e.printStackTrace();
            return null;
        }

    }
}
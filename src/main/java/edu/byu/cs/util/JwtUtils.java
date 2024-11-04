package edu.byu.cs.util;

import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class JwtUtils {
    private static final SecretKey key = generateSecretKey();

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtils.class);

    /**
     * Generates a JWT token for a given netId
     *
     * @param netId the netId to generate a token for
     * @return the generated token, or null if the netId is null
     */
    public static String generateToken(String netId) {
        if (netId == null) {
            return null;
        }
        return Jwts.builder()
                .subject(netId)
                .expiration(Date.from(Instant.now().plus(4, ChronoUnit.HOURS))) // expires 4 hours from now
                .signWith(key)
                .compact();
    }

    /**
     * Validates a JWT and returns the subject if valid (netID)
     *
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
        } catch (Exception e) {
            return null;
        }
    }

    private static SecretKey generateSecretKey() {
        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance("HmacSHA512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to setup key JWT key generator", e);
        }
        keyGenerator.init(512, new SecureRandom());
        return keyGenerator.generateKey();
    }
}

package edu.byu.cs.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * A utility class that handles <a href="https://jwt.io/introduction">JSON Web Tokens</a>
 * (or JWTs for short) "for securely transmitting information between parties as a JSON object."
 * This class provides methods for generating and validating tokens to ensure authentication and
 * authorization of users.
 */
public class JwtUtils {
    private static final SecretKey key = generateSecretKey();
    private static JwkSet byuPublicKeys;

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

    public static String validateTokenAgainstKeys(String token){
        Locator<Key> locator = header -> {
            for (Jwk<?> key : byuPublicKeys) {
                if (header instanceof ProtectedHeader protectedHeader) {
                    if (protectedHeader.getKeyId().equals(key.getId())) {
                        return key.toKey();
                    }
                }
            }
            return null;
        };
        String netId;
            netId = Jwts.parser()
                    .keyLocator(locator)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        return netId;
    }

    public static void readJWKs(String json){
        byuPublicKeys = Jwks.setParser()
                .build()
                .parse(json);
    }


}

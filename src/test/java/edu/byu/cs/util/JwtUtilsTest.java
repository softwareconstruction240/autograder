package edu.byu.cs.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.security.DefaultJwkSet;
import io.jsonwebtoken.security.*;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.security.KeyPair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

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

    @Test
    void validateTokenAgainstKeys() {

    }

    private HashMap<Integer,KeyPair> generateKeyPairs(int size) throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(256);
        HashMap<Integer, KeyPair> pairs = new HashMap<>();
        for (int i = 0; i < size; i++){
            KeyPair pair = generator.generateKeyPair();
            pairs.put(i, pair);
        }
        return pairs;
    }

    private JwkSet generateJwks(HashMap<Integer,KeyPair> pairs) {
        HashSet<Jwk<?>> set = new HashSet<>();
            for (int i = 0; i < pairs.size(); i++){
                KeyPair pair = pairs.get(i);
                Jwk<?> jwk = Jwks.builder()
                        .key(pair.getPublic())
                        .id(Integer.toString(i))
                        .build();
                set.add(jwk);
            }
            JwkSet jwks = Jwks.set().add(set).build();
        return jwks;
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

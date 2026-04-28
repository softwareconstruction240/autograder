package edu.byu.cs.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.jsonwebtoken.security.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.KeyPair;
import io.jsonwebtoken.security.SignatureException;
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

    @ParameterizedTest(name = "validateTokenAgainst{0}Keys")
    @ValueSource(ints = {1, 2, 3})
    void validateTokenAgainstKeys(int size) throws Exception{
        HashMap<Integer, KeyPair> map = generateKeyPairs(size);
        JwkSet set = generateJwks(map);

        String token = generateToken(map.get(size-1).getPrivate(), size-1);
        byte[] bytes =  new JacksonSerializer<JwkSet>().serialize(set);
        String serialized = new String(bytes, StandardCharsets.UTF_8);
        JwtUtils.readJWKs(serialized);
        String netId = JwtUtils.validateTokenAgainstKeys(token);
        assertEquals("testNetId", netId);
    }

    @Test
    void invalidTokenNotVerifiedByAnyKey() throws Exception{
        HashMap<Integer, KeyPair> map = generateKeyPairs(3);
        JwkSet set = generateJwks(map);

        //sign with a fake key
        KeyPair fake = generateKeyPairs(1).get(0);

        String token = generateToken(fake.getPrivate(), 2);
        byte[] bytes =  new JacksonSerializer<JwkSet>().serialize(set);
        String serialized = new String(bytes, StandardCharsets.UTF_8);
        JwtUtils.readJWKs(serialized);
        assertThrows(SignatureException.class, ()-> JwtUtils.validateTokenAgainstKeys(token));
    }

    private HashMap<Integer,KeyPair> generateKeyPairs(int size) throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
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

    private String generateToken(PrivateKey key, int id){
        return Jwts.builder()
                .header()
                .keyId(Integer.toString(id))
                .and()
                .subject("testNetId")
                .signWith(key)
                .compact();
    }

}

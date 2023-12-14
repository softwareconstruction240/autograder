package edu.byu.cs.controller.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class JwtUtils {

    //FIXME: move key to external config
    private static final SecretKey key = Jwts.SIG.HS256.key().build();

    public static String generateToken(String netId) {
        return Jwts.builder()
                .subject(netId)
                .expiration(Date.from(Instant.now().plus(4, ChronoUnit.HOURS))) // expires 4 hours from now
                .signWith(key)
                .compact();
    }

    public static boolean validateToken(String token) {

        try {
            Jwts.parser()
                    .verifyWith(key).
                    build()
                    .parse(token);
            return true;
        } catch (JwtException e) {
            e.printStackTrace();
            return false;
        }

    }
}

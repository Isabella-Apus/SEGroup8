package com.segroup8.secondhand.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

public final class TestJwt {
    public static final String SECRET = "test-jwt-secret-must-have-at-least-thirty-two-bytes";

    private TestJwt() {
    }

    public static String bearer(long userId, String username) {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .claim("uid", userId)
                .claim("username", username)
                .claim("role", "USER")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
        return "Bearer " + token;
    }
}

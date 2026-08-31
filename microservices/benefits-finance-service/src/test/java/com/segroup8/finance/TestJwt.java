package com.segroup8.finance;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

final class TestJwt {
    static final String SECRET = "SEGROUP8_TEST_SECRET_KEY_AT_LEAST_32_BYTES_LONG";

    private TestJwt() {}

    static String bearer(long userId, String role) {
        String token = Jwts.builder().claim("uid", userId).claim("username", "test-" + userId)
                .claim("role", role).issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        return "Bearer " + token;
    }
}

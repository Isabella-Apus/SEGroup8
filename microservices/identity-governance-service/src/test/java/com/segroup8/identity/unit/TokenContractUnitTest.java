package com.segroup8.identity.unit;

import com.segroup8.security.JwtPrincipal;
import com.segroup8.security.JwtTokenVerifier;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class TokenContractUnitTest {
    private static final String SECRET = "test-jwt-secret-with-at-least-32-bytes-2026";

    @Test
    void issuedClaimsRemainCompatibleWithSharedVerifier() {
        String token = Jwts.builder().claim("uid", 7L).claim("username", "alice").claim("role", "USER")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();

        JwtPrincipal principal = new JwtTokenVerifier(SECRET).verifyToken(token);

        assertThat(principal.userId()).isEqualTo(7L);
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(principal.role()).isEqualTo("USER");
    }
}

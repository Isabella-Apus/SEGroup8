package com.segroup8.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenVerifierTest {

    private static final String SECRET = "01234567890123456789012345678901";
    private final SecretKey signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private final JwtTokenVerifier verifier = new JwtTokenVerifier(SECRET);

    @Test
    void verify_shouldReadClaimsFromBackendCompatibleToken() {
        String token = token(Instant.now().plusSeconds(600));

        JwtPrincipal principal = verifier.verifyAuthorizationHeader("Bearer " + token);

        assertEquals(7L, principal.userId());
        assertEquals("member-a", principal.username());
        assertEquals("ADMIN", principal.role());
    }

    @Test
    void verify_shouldRejectTamperedToken() {
        String token = token(Instant.now().plusSeconds(600));

        assertThrows(JwtVerificationException.class,
                () -> verifier.verifyToken(token + "tampered"));
    }

    @Test
    void verify_shouldRejectExpiredToken() {
        String token = token(Instant.now().minusSeconds(1));

        assertThrows(JwtVerificationException.class, () -> verifier.verifyToken(token));
    }

    @Test
    void verify_shouldRejectMissingOrMalformedAuthorizationHeader() {
        assertThrows(JwtVerificationException.class,
                () -> verifier.verifyAuthorizationHeader(null));
        assertThrows(JwtVerificationException.class,
                () -> verifier.verifyAuthorizationHeader("Basic abc"));
        assertThrows(JwtVerificationException.class,
                () -> verifier.verifyAuthorizationHeader("Bearer "));
    }

    @Test
    void constructor_shouldRejectWeakSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenVerifier("too-short"));
    }

    private String token(Instant expiry) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("uid", 7L)
                .claim("username", "member-a")
                .claim("role", "ADMIN")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }
}

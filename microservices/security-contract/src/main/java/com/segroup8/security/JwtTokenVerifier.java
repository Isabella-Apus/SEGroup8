package com.segroup8.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 独立于具体业务服务的 JWT 校验器。
 *
 * <p>各微服务只需要注入相同的 JWT secret，并在网关/过滤器中调用
 * {@link #verifyAuthorizationHeader(String)}，即可得到统一的 uid、username、role。</p>
 */
public final class JwtTokenVerifier {

    private final SecretKey signingKey;

    public JwtTokenVerifier(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 UTF-8 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public JwtPrincipal verifyAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new JwtVerificationException("Missing Bearer token");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new JwtVerificationException("Missing Bearer token");
        }
        return verifyToken(token);
    }

    public JwtPrincipal verifyToken(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtVerificationException("Missing JWT");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new JwtPrincipal(
                    readUserId(claims.get("uid")),
                    readRequiredText(claims, "username"),
                    readRequiredText(claims, "role"));
        } catch (JwtVerificationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtVerificationException("Invalid or expired JWT", ex);
        }
    }

    private long readUserId(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                // Fall through to the uniform missing/invalid claim error.
            }
        }
        throw new JwtVerificationException("JWT uid claim is missing or invalid");
    }

    private String readRequiredText(Claims claims, String name) {
        String value = claims.get(name, String.class);
        if (value == null || value.isBlank()) {
            throw new JwtVerificationException("JWT " + name + " claim is missing");
        }
        return value;
    }
}

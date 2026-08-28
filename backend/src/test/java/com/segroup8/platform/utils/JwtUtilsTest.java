package com.segroup8.platform.utils;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("PLATFORM")
class JwtUtilsTest {

    @Test
    void createAndParse_shouldPreserveClaims() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("SEGROUP8_TEST_SECRET_KEY_2026_LONG_ENOUGH");
        properties.setExpireHours(1L);
        JwtUtils jwtUtils = new JwtUtils(properties);

        String token = jwtUtils.createToken(7L, "member-a", "ADMIN");
        Claims claims = jwtUtils.parseToken(token);

        assertEquals(7L, ((Number) claims.get("uid")).longValue());
        assertEquals("member-a", claims.get("username"));
        assertEquals("ADMIN", claims.get("role"));
    }

    @Test
    void parse_shouldRejectTamperedToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("SEGROUP8_TEST_SECRET_KEY_2026_LONG_ENOUGH");
        properties.setExpireHours(1L);
        JwtUtils jwtUtils = new JwtUtils(properties);
        String token = jwtUtils.createToken(7L, "member-a", "USER");

        assertThrows(BusinessException.class, () -> jwtUtils.parseToken(token + "tampered"));
    }
}

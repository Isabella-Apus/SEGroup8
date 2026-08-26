package com.segroup8.platform.interceptor;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthInterceptorTest {

    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Claims claims;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void preHandle_shouldRejectMissingBearerHeader() {
        when(request.getHeader("Authorization")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> new JwtAuthInterceptor(jwtUtils).preHandle(request, response, new Object()));

        assertEquals(401, ex.getCode());
    }

    @Test
    void preHandle_shouldParseTokenAndPopulateUserContext() {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtils.parseToken("valid-token")).thenReturn(claims);
        when(claims.get("uid")).thenReturn(7L);

        boolean allowed = new JwtAuthInterceptor(jwtUtils).preHandle(request, response, new Object());

        assertTrue(allowed);
        assertEquals(7L, UserContext.getUserId());
    }

    @Test
    void preHandle_shouldPropagateInvalidTokenAsUnauthorized() {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtUtils.parseToken("invalid-token"))
                .thenThrow(new BusinessException(401, "无效或过期的登录令牌"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> new JwtAuthInterceptor(jwtUtils).preHandle(request, response, new Object()));

        assertEquals(401, ex.getCode());
    }
}

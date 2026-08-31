package com.segroup8.platform.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Authenticates service-to-service calls without accepting an end-user JWT. */
@Component
public class InternalServiceInterceptor implements HandlerInterceptor {
    private final String expectedToken;

    public InternalServiceInterceptor(@Value("${app.internal-service.token:}") String expectedToken) {
        this.expectedToken = expectedToken == null ? "" : expectedToken;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String actual = request.getHeader("X-Internal-Service-Token");
        if (expectedToken.isBlank() || actual == null
                || !MessageDigest.isEqual(expectedToken.getBytes(StandardCharsets.UTF_8),
                        actual.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            try {
                response.getWriter().write("{\"code\":401,\"message\":\"Valid service identity is required\"}");
            } catch (java.io.IOException ex) {
                // The response is already unauthorized; there is nothing useful to recover here.
            }
            return false;
        }
        if (isBlank(request.getHeader("X-Request-Id"))
                || isBlank(request.getHeader("X-Idempotency-Key"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            try {
                response.getWriter().write("{\"code\":400,\"message\":\"Request identity headers are required\"}");
            } catch (java.io.IOException ex) {
                // The response is already invalid; there is nothing useful to recover here.
            }
            return false;
        }
        return true;
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}

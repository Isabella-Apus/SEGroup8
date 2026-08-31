package com.segroup8.identity.security;

import com.segroup8.identity.api.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalServiceInterceptor implements HandlerInterceptor {
    private final byte[] expectedToken;

    public InternalServiceInterceptor(@Value("${app.internal-service-token}") String token) {
        this.expectedToken = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String actual = request.getHeader("X-Internal-Service-Token");
        if (actual == null || !MessageDigest.isEqual(expectedToken, actual.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(403, "内部服务身份无效");
        }
        if (request.getHeader("X-Request-Id") == null || request.getHeader("X-Request-Id").isBlank()) {
            throw new ApiException(400, "缺少 X-Request-Id");
        }
        if (!"GET".equalsIgnoreCase(request.getMethod())
                && (request.getHeader("X-Idempotency-Key") == null
                || request.getHeader("X-Idempotency-Key").isBlank())) {
            throw new ApiException(400, "缺少 X-Idempotency-Key");
        }
        return true;
    }
}

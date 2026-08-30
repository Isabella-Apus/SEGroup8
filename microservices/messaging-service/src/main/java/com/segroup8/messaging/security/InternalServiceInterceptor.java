package com.segroup8.messaging.security;

import com.segroup8.messaging.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InternalServiceInterceptor implements HandlerInterceptor {
    public static final String HEADER = "X-Internal-Service-Token";
    public static final String IDENTITY_HEADER = "X-Service-Identity";
    public static final String IDENTITY_ATTRIBUTE = InternalServiceInterceptor.class.getName() + ".identity";
    private final String expected;
    private final String operationsToken;

    public InternalServiceInterceptor(@Value("${app.internal-service.token:}") String expected,
            @Value("${app.internal-service.operations-token:}") String operationsToken) {
        this.expected = expected;
        this.operationsToken = operationsToken;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String actual = request.getHeader(HEADER);
        boolean replay = request.getRequestURI().startsWith("/internal/events/replay/");
        String required = replay ? operationsToken : expected;
        if (required == null || required.isBlank() || actual == null ||
                !MessageDigest.isEqual(required.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(401, "Valid service identity is required");
        }
        String identity = request.getHeader(IDENTITY_HEADER);
        request.setAttribute(IDENTITY_ATTRIBUTE,
                identity == null || identity.isBlank() ? "authenticated-service" : identity.trim());
        return true;
    }
}

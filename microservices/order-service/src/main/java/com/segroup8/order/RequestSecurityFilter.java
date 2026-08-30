package com.segroup8.order;

import com.segroup8.security.JwtPrincipal;
import com.segroup8.security.JwtTokenVerifier;
import com.segroup8.security.JwtVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class RequestSecurityFilter extends OncePerRequestFilter {
    static final String PRINCIPAL = RequestSecurityFilter.class.getName() + ".principal";
    private final JwtTokenVerifier verifier;
    private final String internalToken;
    private final boolean testHeaders;

    RequestSecurityFilter(JwtTokenVerifier verifier,
            @Value("${security.internal-service-token}") String internalToken,
            @Value("${security.test-headers-enabled:false}") boolean testHeaders) {
        this.verifier = verifier;
        this.internalToken = internalToken;
        this.testHeaders = testHeaders;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = Optional.ofNullable(request.getHeader("X-Request-Id"))
                .filter(v -> !v.isBlank()).orElseGet(() -> UUID.randomUUID().toString());
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        try {
            if (request.getRequestURI().startsWith("/internal/")) {
                if (!constantTimeEquals(internalToken, request.getHeader("X-Internal-Service-Token"))) {
                    reject(response, 401, "INVALID_INTERNAL_TOKEN");
                    return;
                }
            } else if (request.getRequestURI().startsWith("/api/")) {
                try {
                    JwtPrincipal principal;
                    if (testHeaders && request.getHeader("X-User-Id") != null) {
                        principal = new JwtPrincipal(Long.parseLong(request.getHeader("X-User-Id")), "test",
                                Optional.ofNullable(request.getHeader("X-User-Role")).orElse("USER"));
                    } else {
                        principal = verifier.verifyAuthorizationHeader(request.getHeader("Authorization"));
                    }
                    request.setAttribute(PRINCIPAL, principal);
                } catch (JwtVerificationException | NumberFormatException ex) {
                    reject(response, 401, "UNAUTHORIZED");
                    return;
                }
            }
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    static JwtPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(PRINCIPAL);
        if (value instanceof JwtPrincipal principal) return principal;
        throw new OrderException("UNAUTHORIZED", "Authentication is required", 401);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null || expected.length() != actual.length()) return false;
        int result = 0;
        for (int i = 0; i < expected.length(); i++) result |= expected.charAt(i) ^ actual.charAt(i);
        return result == 0;
    }

    private void reject(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"request rejected\",\"data\":null}");
    }
}

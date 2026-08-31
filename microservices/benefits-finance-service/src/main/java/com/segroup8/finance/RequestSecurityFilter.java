package com.segroup8.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.finance.ApiModels.ErrorResponse;
import com.segroup8.security.JwtPrincipal;
import com.segroup8.security.JwtTokenVerifier;
import com.segroup8.security.JwtVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
final class RequestSecurityFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RequestSecurityFilter.class);
    private final byte[] internalToken;
    private final JwtTokenVerifier jwtVerifier;
    private final ObjectMapper json;

    RequestSecurityFilter(@Value("${app.internal-service-token}") String token,
            @Value("${app.jwt-secret}") String jwtSecret, ObjectMapper json) {
        this.internalToken = token.getBytes(StandardCharsets.UTF_8);
        this.jwtVerifier = new JwtTokenVerifier(jwtSecret);
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String path = request.getRequestURI();
        String requestId = normalizeRequestId(request.getHeader("X-Request-Id"));
        String traceId = normalizeRequestId(request.getHeader("X-Trace-Id"));
        response.setHeader("X-Request-Id", requestId);
        response.setHeader("X-Trace-Id", traceId);
        MDC.put("requestId", requestId);
        MDC.put("traceId", traceId);
        RequestContext.setTraceId(traceId);
        try {
            if (path.startsWith("/internal/")) {
                byte[] provided = value(request.getHeader("X-Internal-Service-Token"))
                        .getBytes(StandardCharsets.UTF_8);
                if (!MessageDigest.isEqual(internalToken, provided)) {
                    reject(response, 403, "SERVICE_IDENTITY_FORBIDDEN", "内部接口仅允许受信任的集群服务调用", requestId);
                    return;
                }
            } else if (path.startsWith("/api/")) {
                JwtPrincipal principal;
                try {
                    principal = jwtVerifier.verifyAuthorizationHeader(request.getHeader("Authorization"));
                } catch (JwtVerificationException error) {
                    reject(response, 401, "AUTH_REQUIRED", "缺少、无效或过期的 Bearer JWT", requestId);
                    return;
                }
                String role = principal.role().toUpperCase(Locale.ROOT);
                RequestContext.set(new RequestContext.Caller(principal.userId(), role, requestId));
                MDC.put("userId", mask(principal.userId()));
            }
            chain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
            LOG.atInfo()
                    .addKeyValue("event", "http_request_completed")
                    .addKeyValue("method", request.getMethod())
                    .addKeyValue("path", path)
                    .addKeyValue("status", response.getStatus())
                    .addKeyValue("durationMs", durationMs)
                    .log("http_request_completed");
            RequestContext.clear();
            MDC.clear();
        }
    }

    private void reject(HttpServletResponse response, int status, String code, String message, String requestId)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getOutputStream(), new ErrorResponse(code, message, requestId, Instant.now()));
    }

    private static String value(String value) { return value == null ? "" : value.trim(); }
    private static String normalizeRequestId(String value) {
        String normalized = value(value);
        return normalized.matches("[A-Za-z0-9._:-]{1,80}") ? normalized : UUID.randomUUID().toString();
    }
    private static String mask(long userId) {
        String value = Long.toString(userId);
        return value.length() < 5 ? "***" : value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
}

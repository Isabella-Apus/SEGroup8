package com.segroup8.secondhand.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = valueOrGenerate(request.getHeader("X-Request-Id"));
        String traceId = valueOrGenerate(request.getHeader("X-Trace-Id"));
        try (MDC.MDCCloseable ignoredRequest = MDC.putCloseable("requestId", requestId);
                MDC.MDCCloseable ignoredTrace = MDC.putCloseable("traceId", traceId)) {
            response.setHeader("X-Request-Id", requestId);
            response.setHeader("X-Trace-Id", traceId);
            filterChain.doFilter(request, response);
        }
    }

    private String valueOrGenerate(String value) {
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.substring(0, Math.min(100, value.length()));
    }
}

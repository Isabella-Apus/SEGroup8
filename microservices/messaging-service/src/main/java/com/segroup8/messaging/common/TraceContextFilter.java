package com.segroup8.messaging.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Propagates a safe correlation id without ever logging credentials or bodies. */
@Component
public class TraceContextFilter extends OncePerRequestFilter {
    public static final String TRACE_HEADER = "X-Trace-Id";
    private static final Logger LOG = LoggerFactory.getLogger(TraceContextFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String supplied = request.getHeader(TRACE_HEADER);
        String traceId = supplied == null || supplied.isBlank() ? UUID.randomUUID().toString() : supplied.trim();
        if (traceId.length() > 128) traceId = traceId.substring(0, 128);
        MDC.put("traceId", traceId);
        MDC.put("requestId", traceId);
        response.setHeader(TRACE_HEADER, traceId);
        long started = System.nanoTime();
        try { filterChain.doFilter(request, response); }
        finally {
            LOG.info("http_request_completed method={} path={} status={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(),
                    (System.nanoTime() - started) / 1_000_000L);
            MDC.remove("traceId"); MDC.remove("requestId");
        }
    }
}

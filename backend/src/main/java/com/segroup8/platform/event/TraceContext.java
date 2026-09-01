package com.segroup8.platform.event;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class TraceContext {
    public static final String HEADER = "X-Trace-Id";
    private static final String ATTRIBUTE = TraceContext.class.getName() + ".traceId";

    private TraceContext() { }

    public static String currentTraceId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String value = request.getHeader(HEADER);
            if (value != null && !value.isBlank()) return value.trim();
            Object interceptorTrace = request.getAttribute("traceId");
            if (interceptorTrace instanceof String existing && !existing.isBlank()) return existing;
            Object generated = request.getAttribute(ATTRIBUTE);
            if (generated instanceof String existing && !existing.isBlank()) return existing;
            String created = UUID.randomUUID().toString();
            request.setAttribute(ATTRIBUTE, created);
            return created;
        }
        return UUID.randomUUID().toString();
    }
}

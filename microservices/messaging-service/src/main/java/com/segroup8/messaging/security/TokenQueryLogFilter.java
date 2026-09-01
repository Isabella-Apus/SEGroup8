package com.segroup8.messaging.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

/** Prevents framework DEBUG logs from exposing WebSocket query JWTs. */
public class TokenQueryLogFilter extends TurboFilter {
    private static final String SENSITIVE_PATH = "/ws/realtime?token=";

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format,
            Object[] params, Throwable throwable) {
        if (containsTokenQuery(format)) return FilterReply.DENY;
        if (params != null) {
            for (Object param : params) if (param != null && containsTokenQuery(param.toString())) return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }

    private boolean containsTokenQuery(String value) {
        return value != null && value.contains(SENSITIVE_PATH);
    }
}

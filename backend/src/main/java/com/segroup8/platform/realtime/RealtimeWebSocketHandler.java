package com.segroup8.platform.realtime;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtils jwtUtils;
    private final RealtimePushService realtimePushService;

    public RealtimeWebSocketHandler(JwtUtils jwtUtils, RealtimePushService realtimePushService) {
        this.jwtUtils = jwtUtils;
        this.realtimePushService = realtimePushService;
    }

    @Override
    @SuppressWarnings("null")
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String token = getToken(session.getUri());
        if (!StringUtils.hasText(token)) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        try {
            Claims claims = jwtUtils.parseToken(token);
            Object uidObj = claims.get("uid");
            Long userId = uidObj == null ? null : Long.valueOf(String.valueOf(uidObj));
            if (userId == null) {
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }
            realtimePushService.register(userId, session);
            session.sendMessage(new TextMessage("{\"eventType\":\"CONNECTED\",\"payload\":{\"ok\":true}}"));
        } catch (BusinessException ex) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        realtimePushService.unregister(session);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        // heartbeat passthrough
    }

    private String getToken(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        String[] parts = uri.getQuery().split("&");
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = part.substring(0, idx);
            if ("token".equals(key)) {
                return URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}

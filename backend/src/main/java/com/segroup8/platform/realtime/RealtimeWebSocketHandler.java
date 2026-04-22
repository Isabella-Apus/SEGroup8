package com.segroup8.platform.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.service.ChatService;
import com.segroup8.platform.vo.ChatMessageVO;
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
import java.util.Map;

@Component
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private static final String USER_ID_ATTR = "uid";

    private final JwtUtils jwtUtils;
    private final RealtimePushService realtimePushService;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public RealtimeWebSocketHandler(JwtUtils jwtUtils,
            RealtimePushService realtimePushService,
            ChatService chatService,
            ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.realtimePushService = realtimePushService;
        this.chatService = chatService;
        this.objectMapper = objectMapper;
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
        Object uidObj = session.getAttributes().get(USER_ID_ATTR);
        Long userId = uidObj == null ? null : Long.valueOf(String.valueOf(uidObj));
        if (userId == null) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String eventType = root.path("eventType").asText("");
            if ("PING".equalsIgnoreCase(eventType)) {
                session.sendMessage(new TextMessage("{\"eventType\":\"PONG\"}"));
                return;
            }
            if (!"CHAT_SEND".equalsIgnoreCase(eventType)) {
                return;
            }
            JsonNode payload = root.path("payload");
            Long conversationId = payload.path("conversationId").isNumber()
                    ? payload.path("conversationId").asLong()
                    : null;
            String content = payload.path("content").asText("");
            ChatMessageVO chatMessage = chatService.sendMessage(userId, conversationId, content);
            realtimePushService.pushToUsers(
                    java.util.List.of(chatMessage.getSenderUserId(), chatMessage.getReceiverUserId()),
                    "CHAT_MESSAGE",
                    chatMessage);
        } catch (BusinessException ex) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "eventType", "CHAT_ERROR",
                        "payload", Map.of("message", ex.getMessage())))));
            } catch (Exception ignored) {
                // ignore secondary send failure
            }
        } catch (Exception ignored) {
            // ignore malformed payload
        }
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

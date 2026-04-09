package com.segroup8.platform.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RealtimePushService {

    private static final String USER_ID_ATTR = "uid";
    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public RealtimePushService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(Long userId, WebSocketSession session) {
        if (userId == null || session == null) {
            return;
        }
        session.getAttributes().put(USER_ID_ATTR, userId);
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(WebSocketSession session) {
        if (session == null) {
            return;
        }
        Object uidObj = session.getAttributes().get(USER_ID_ATTR);
        if (!(uidObj instanceof Long uid)) {
            return;
        }
        Set<WebSocketSession> sessions = userSessions.get(uid);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            userSessions.remove(uid);
        }
    }

    @SuppressWarnings("null")
    public void pushToUser(Long userId, String eventType, Object payload) {
        if (userId == null || !StringUtils.hasText(eventType)) {
            return;
        }
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        String json = toJson(Map.of(
                "eventType", eventType,
                "timestamp", LocalDateTime.now().toString(),
                "payload", payload
        ));
        if (json == null) {
            return;
        }
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException ignored) {
                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (IOException ignoredClose) {
                    // ignore close failure
                }
            }
        }
    }

    public void pushToUsers(Iterable<Long> userIds, String eventType, Object payload) {
        if (userIds == null) {
            return;
        }
        for (Long userId : userIds) {
            pushToUser(userId, eventType, payload);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }
}

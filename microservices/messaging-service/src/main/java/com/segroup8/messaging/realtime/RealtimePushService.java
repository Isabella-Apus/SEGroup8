package com.segroup8.messaging.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
public class RealtimePushService implements RealtimePublisher {
    private final ObjectMapper json;
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    public RealtimePushService(ObjectMapper json) { this.json = json; }

    public void register(long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }
    public void unregister(WebSocketSession session) {
        sessions.values().forEach(values -> values.remove(session));
        sessions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    public void disconnectUser(long userId) {
        Set<WebSocketSession> values = sessions.remove(userId);
        if (values == null) return;
        values.forEach(session -> close(session, CloseStatus.POLICY_VIOLATION));
    }
    public int sessionCount(long userId) { return sessions.getOrDefault(userId, Set.of()).size(); }

    public void pushToUser(long userId, String eventType, Object payload) {
        Set<WebSocketSession> values = sessions.get(userId);
        if (values == null || values.isEmpty()) return;
        TextMessage message = new TextMessage(serialize(Map.of(
                "eventType", eventType, "timestamp", LocalDateTime.now().toString(), "payload", payload)));
        for (WebSocketSession session : Set.copyOf(values)) {
            if (!session.isOpen()) { unregister(session); continue; }
            try { session.sendMessage(message); }
            catch (IOException | RuntimeException ex) { unregister(session); close(session, CloseStatus.SERVER_ERROR); }
        }
    }
    public void pushToUsers(Iterable<Long> userIds, String eventType, Object payload) {
        if (userIds != null) userIds.forEach(id -> pushToUser(id, eventType, payload));
    }
    private String serialize(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("Realtime payload cannot be serialized", ex); }
    }
    private void close(WebSocketSession session, CloseStatus status) {
        try { if (session.isOpen()) session.close(status); } catch (IOException ignored) { }
    }
}

package com.segroup8.messaging.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.messaging.common.MessagingMetrics;
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
    private final MessagingMetrics metrics;
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    public RealtimePushService(ObjectMapper json, MessagingMetrics metrics) { this.json = json; this.metrics = metrics; }

    public void register(long userId, WebSocketSession session) {
        if (sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session)) metrics.sessionOpened();
    }
    public void unregister(WebSocketSession session) {
        sessions.values().forEach(values -> { if (values.remove(session)) metrics.sessionClosed(); });
        sessions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    public void disconnectUser(long userId) {
        Set<WebSocketSession> values = sessions.remove(userId);
        if (values == null) return;
        values.forEach(ignored -> metrics.sessionClosed());
        values.forEach(session -> close(session, CloseStatus.POLICY_VIOLATION));
    }
    public int sessionCount(long userId) { return sessions.getOrDefault(userId, Set.of()).size(); }

    public void pushToUser(long userId, String eventType, Object payload) {
        tryPushToUser(userId, eventType, payload);
    }
    public boolean tryPushToUser(long userId, String eventType, Object payload) {
        Set<WebSocketSession> values = sessions.get(userId);
        if (values == null || values.isEmpty()) return false;
        TextMessage message = new TextMessage(serialize(Map.of(
                "eventType", eventType, "timestamp", LocalDateTime.now().toString(), "payload", payload)));
        boolean delivered = false;
        for (WebSocketSession session : Set.copyOf(values)) {
            if (!session.isOpen()) { unregister(session); continue; }
            try { session.sendMessage(message); delivered = true; }
            catch (IOException | RuntimeException ex) { metrics.pushFailed(); unregister(session); close(session, CloseStatus.SERVER_ERROR); }
        }
        return delivered;
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

package com.segroup8.messaging.realtime;

import org.springframework.web.socket.WebSocketSession;

public interface RealtimePublisher {
    void register(long userId, WebSocketSession session);
    void unregister(WebSocketSession session);
    void disconnectUser(long userId);
    int sessionCount(long userId);
    void pushToUser(long userId, String eventType, Object payload);
    void pushToUsers(Iterable<Long> userIds, String eventType, Object payload);
}

package com.segroup8.messaging.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.messaging.chat.ChatService;
import com.segroup8.messaging.common.ApiException;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class RealtimeWebSocketHandler extends TextWebSocketHandler {
    private final RealtimePublisher realtime;
    private final ChatService chat;
    private final ObjectMapper json;
    public RealtimeWebSocketHandler(RealtimePublisher realtime, ChatService chat, ObjectMapper json) {
        this.realtime = realtime; this.chat = chat; this.json = json;
    }
    @Override public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object value = session.getAttributes().get(RealtimeHandshakeInterceptor.USER_ID);
        if (!(value instanceof Number number)) { session.close(CloseStatus.NOT_ACCEPTABLE); return; }
        realtime.register(number.longValue(), session);
        session.sendMessage(new TextMessage("{\"eventType\":\"CONNECTED\",\"payload\":{\"ok\":true}}"));
    }
    @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Object value = session.getAttributes().get(RealtimeHandshakeInterceptor.USER_ID);
        if (!(value instanceof Number number)) return;
        try {
            JsonNode root = json.readTree(message.getPayload());
            String type = root.path("eventType").asText();
            if ("PING".equalsIgnoreCase(type)) { session.sendMessage(new TextMessage("{\"eventType\":\"PONG\"}")); return; }
            if (!"CHAT_SEND".equalsIgnoreCase(type)) return;
            JsonNode payload = root.path("payload");
            if (!payload.path("conversationId").canConvertToLong()) throw new ApiException(400, "conversationId is required");
            chat.send(number.longValue(), payload.path("conversationId").asLong(), payload.path("content").asText());
        } catch (ApiException ex) {
            sendError(session, ex.getMessage());
        } catch (IOException ex) {
            sendError(session, "Malformed realtime message");
        }
    }
    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { realtime.unregister(session); }
    @Override public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        realtime.unregister(session); if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }
    private void sendError(WebSocketSession session, String message) throws IOException {
        session.sendMessage(new TextMessage(json.writeValueAsString(Map.of(
                "eventType", "CHAT_ERROR", "payload", Map.of("message", message)))));
    }
}

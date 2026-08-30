package com.segroup8.messaging.config;

import com.segroup8.messaging.realtime.RealtimeHandshakeInterceptor;
import com.segroup8.messaging.realtime.RealtimeWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final RealtimeWebSocketHandler handler;
    private final RealtimeHandshakeInterceptor handshake;
    private final String[] origins;
    public WebSocketConfig(RealtimeWebSocketHandler handler, RealtimeHandshakeInterceptor handshake,
            @Value("${app.realtime.allowed-origin-patterns}") String origins) {
        this.handler = handler; this.handshake = handshake; this.origins = origins.split("\\s*,\\s*");
    }
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/realtime").addInterceptors(handshake).setAllowedOriginPatterns(origins);
    }
}

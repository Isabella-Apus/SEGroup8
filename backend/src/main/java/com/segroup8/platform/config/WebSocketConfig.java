package com.segroup8.platform.config;

import com.segroup8.platform.realtime.RealtimeWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final @NonNull RealtimeWebSocketHandler realtimeWebSocketHandler;
    private final String[] allowedOriginPatterns;

    public WebSocketConfig(@NonNull RealtimeWebSocketHandler realtimeWebSocketHandler,
                           @Value("${app.realtime.allowed-origin-patterns:http://127.0.0.1:5173,http://localhost:5173}") String allowedOrigins) {
        this.realtimeWebSocketHandler = realtimeWebSocketHandler;
        this.allowedOriginPatterns = allowedOrigins.split("\\s*,\\s*");
    }

    @Override
    @SuppressWarnings("null")
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeWebSocketHandler, "/ws/realtime")
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }
}

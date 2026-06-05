package com.segroup8.platform.config;

import com.segroup8.platform.realtime.RealtimeWebSocketHandler;
import com.segroup8.platform.realtime.RealtimeHandshakeInterceptor;
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
    private final @NonNull RealtimeHandshakeInterceptor realtimeHandshakeInterceptor;
    private final String[] allowedOriginPatterns;

    public WebSocketConfig(@NonNull RealtimeWebSocketHandler realtimeWebSocketHandler,
                           @NonNull RealtimeHandshakeInterceptor realtimeHandshakeInterceptor,
                           @Value("${app.realtime.allowed-origin-patterns:*}") String allowedOrigins) {
        this.realtimeWebSocketHandler = realtimeWebSocketHandler;
        this.realtimeHandshakeInterceptor = realtimeHandshakeInterceptor;
        this.allowedOriginPatterns = allowedOrigins.split("\\s*,\\s*");
    }

    @Override
    @SuppressWarnings("null")
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeWebSocketHandler, "/ws/realtime")
                .addInterceptors(realtimeHandshakeInterceptor)
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }
}

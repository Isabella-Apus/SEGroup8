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
                           @Value("${app.realtime.allowed-origin-patterns:http://127.0.0.1:*,http://localhost:*,http://192.168.*:*,http://10.*:*,http://172.16.*:*,http://172.17.*:*,http://172.18.*:*,http://172.19.*:*,http://172.2*:*,http://172.30.*:*,http://172.31.*:*}") String allowedOrigins) {
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

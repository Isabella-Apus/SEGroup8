package com.segroup8.platform.realtime;

import com.segroup8.platform.config.JwtProperties;
import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("DOMAIN_E")
@Tag("UC25")
class RealtimeHandshakeInterceptorTest {

    private JwtUtils jwtUtils;
    private RealtimeHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-must-be-at-least-32-bytes-long");
        properties.setExpireHours(24L);
        jwtUtils = new JwtUtils(properties);
        interceptor = new RealtimeHandshakeInterceptor(jwtUtils);
    }

    @Test
    void beforeHandshake_shouldAcceptValidTokenAndExposeUserId() {
        String token = jwtUtils.createToken(42L, "buyer", "USER");
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler handler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();
        when(request.getURI()).thenReturn(URI.create("http://localhost/ws/realtime?token=" + token));

        boolean accepted = interceptor.beforeHandshake(request, response, handler, attributes);

        assertTrue(accepted);
        assertEquals(42L, attributes.get(RealtimeHandshakeInterceptor.USER_ID_ATTR));
    }

    @Test
    void beforeHandshake_shouldRejectMissingToken() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler handler = mock(WebSocketHandler.class);
        when(request.getURI()).thenReturn(URI.create("http://localhost/ws/realtime"));

        boolean accepted = interceptor.beforeHandshake(request, response, handler, new HashMap<>());

        assertFalse(accepted);
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void beforeHandshake_shouldRejectTamperedToken() {
        String token = jwtUtils.createToken(42L, "buyer", "USER") + "tampered";
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler handler = mock(WebSocketHandler.class);
        when(request.getURI()).thenReturn(URI.create("http://localhost/ws/realtime?token=" + token));

        boolean accepted = interceptor.beforeHandshake(request, response, handler, new HashMap<>());

        assertFalse(accepted);
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }
}

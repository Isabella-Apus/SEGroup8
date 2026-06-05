package com.segroup8.platform.realtime;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class RealtimeHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTR = "uid";

    private final JwtUtils jwtUtils;

    public RealtimeHandshakeInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = getToken(request.getURI());
        if (!StringUtils.hasText(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            Claims claims = jwtUtils.parseToken(token);
            Object uid = claims.get("uid");
            if (uid == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put(USER_ID_ATTR, Long.valueOf(String.valueOf(uid)));
            return true;
        } catch (BusinessException | NumberFormatException ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // No post-handshake work is required.
    }

    private String getToken(URI uri) {
        if (uri == null || uri.getRawQuery() == null) {
            return null;
        }
        for (String part : uri.getRawQuery().split("&")) {
            int index = part.indexOf('=');
            if (index <= 0 || !"token".equals(part.substring(0, index))) {
                continue;
            }
            return URLDecoder.decode(part.substring(index + 1), StandardCharsets.UTF_8);
        }
        return null;
    }
}

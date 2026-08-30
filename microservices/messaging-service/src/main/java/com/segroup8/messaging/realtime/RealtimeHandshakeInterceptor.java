package com.segroup8.messaging.realtime;

import com.segroup8.messaging.access.AccessPolicy;
import com.segroup8.messaging.common.ApiException;
import com.segroup8.security.JwtPrincipal;
import com.segroup8.security.JwtTokenVerifier;
import com.segroup8.security.JwtVerificationException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class RealtimeHandshakeInterceptor implements HandshakeInterceptor {
    public static final String USER_ID = "uid";
    public static final String TOKEN = "verifiedToken";
    private final JwtTokenVerifier verifier;
    private final AccessPolicy access;
    public RealtimeHandshakeInterceptor(JwtTokenVerifier verifier, AccessPolicy access) {
        this.verifier = verifier; this.access = access;
    }
    @Override public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler handler, Map<String, Object> attributes) {
        String token = token(request.getURI());
        try {
            JwtPrincipal principal = verifier.verifyToken(token);
            access.requireActive(principal.userId());
            attributes.put(USER_ID, principal.userId());
            attributes.put(TOKEN, token);
            return true;
        } catch (JwtVerificationException ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED); return false;
        } catch (ApiException ex) {
            response.setStatusCode(ex.code() == 403 ? HttpStatus.FORBIDDEN : HttpStatus.SERVICE_UNAVAILABLE); return false;
        }
    }
    @Override public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler handler, Exception exception) { }
    private String token(URI uri) {
        if (uri == null || uri.getRawQuery() == null) return null;
        for (String part : uri.getRawQuery().split("&")) {
            int separator = part.indexOf('=');
            if (separator > 0 && "token".equals(part.substring(0, separator)))
                return URLDecoder.decode(part.substring(separator + 1), StandardCharsets.UTF_8);
        }
        return null;
    }
}

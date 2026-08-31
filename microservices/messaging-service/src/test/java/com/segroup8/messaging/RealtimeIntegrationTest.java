package com.segroup8.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.segroup8.messaging.realtime.RealtimePushService;
import com.segroup8.messaging.event.EventEnvelope;
import com.segroup8.messaging.event.EventTypes;
import com.segroup8.messaging.event.InboxEventService;
import com.segroup8.messaging.event.InboxWorker;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealtimeIntegrationTest {
    private static final String SECRET = "TEST_ONLY_MESSAGING_SECRET_12345678901234567890";
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired RealtimePushService realtime;
    @Autowired InboxEventService inbox;
    @Autowired InboxWorker inboxWorker;

    @BeforeEach void seed() {
        jdbc.update("delete from inbox_event");
        jdbc.update("delete from user_access_projection");
        jdbc.update("insert into user_access_projection(user_id,access_status,role,display_name,source_version) " +
                "values(1,'ACTIVE','USER','Alice',1),(4,'BANNED','USER','Banned',1)");
    }

    @Test
    void handshakeEnforcesJwtOriginAndBanAndCleansUpClosedConnections() throws Exception {
        WebSocketSession valid = connect("http://localhost:5174", token(1));
        assertTrue(valid.isOpen());
        valid.sendMessage(new TextMessage("{\"eventType\":\"PING\"}"));
        valid.close();
        awaitNoSessions(1);
        assertThrows(ExecutionException.class, () -> connect("http://localhost:5174", null));
        assertThrows(ExecutionException.class, () -> connect("http://localhost:5174", "invalid"));
        assertThrows(ExecutionException.class, () -> connect("http://localhost:5174", tokenWithoutUid()));
        assertThrows(ExecutionException.class, () -> connect("https://evil.example", token(1)));
        assertThrows(ExecutionException.class, () -> connect("http://localhost:5174", token(4)));
    }

    @Test
    void individualPushFailureIsIsolatedAndSessionIsRemoved() {
        FailingSession failed = new FailingSession();
        realtime.register(1, failed);
        realtime.pushToUser(1, "NOTIFICATION", Map.of("id", 1));
        assertEquals(0, realtime.sessionCount(1));
        assertTrue(failed.closed);
    }

    @Test
    void userAccessEventActivelyDisconnectsExistingSessionAndRejectsReconnect() throws Exception {
        WebSocketSession connected = connect("http://localhost:5174", token(1));
        assertTrue(connected.isOpen());
        EventEnvelope event = new EventEnvelope("ws-ban-event", EventTypes.USER_ACCESS_CHANGED, 1,
                "identity-governance-monolith", "USER", "1", Instant.now(), "ws-ban-trace",
                Map.of("userId", 1L, "status", "BANNED", "role", "USER", "version", 2L));
        assertTrue(inbox.accept(event));
        inboxWorker.runOnce();

        awaitNoSessions(1);
        assertThrows(ExecutionException.class, () -> connect("http://localhost:5174", token(1)));
    }

    private WebSocketSession connect(String origin, String token) throws Exception {
        String suffix = token == null ? "" : "?token=" + token;
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.setOrigin(origin);
        WebSocketHandler handler = new TextWebSocketHandler() {};
        return new StandardWebSocketClient().execute(handler, headers,
                URI.create("ws://127.0.0.1:" + port + "/ws/realtime" + suffix)).get(5, TimeUnit.SECONDS);
    }

    private void awaitNoSessions(long userId) throws InterruptedException {
        for (int i = 0; i < 50 && realtime.sessionCount(userId) != 0; i++) Thread.sleep(10);
        assertEquals(0, realtime.sessionCount(userId));
    }
    private String token(long uid) {
        Instant now = Instant.now();
        return Jwts.builder().claim("uid", uid).claim("username", "u" + uid).claim("role", "USER")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
    private String tokenWithoutUid() {
        Instant now = Instant.now();
        return Jwts.builder().claim("username", "missing").claim("role", "USER")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private static final class FailingSession implements WebSocketSession {
        private boolean open = true;
        private boolean closed;
        private final Map<String, Object> attributes = new java.util.concurrent.ConcurrentHashMap<>();
        @Override public String getId() { return "failing"; }
        @Override public URI getUri() { return URI.create("ws://localhost/ws/realtime"); }
        @Override public HttpHeaders getHandshakeHeaders() { return HttpHeaders.EMPTY; }
        @Override public Map<String, Object> getAttributes() { return attributes; }
        @Override public Principal getPrincipal() { return null; }
        @Override public InetSocketAddress getLocalAddress() { return null; }
        @Override public InetSocketAddress getRemoteAddress() { return null; }
        @Override public String getAcceptedProtocol() { return null; }
        @Override public void setTextMessageSizeLimit(int messageSizeLimit) { }
        @Override public int getTextMessageSizeLimit() { return 64 * 1024; }
        @Override public void setBinaryMessageSizeLimit(int messageSizeLimit) { }
        @Override public int getBinaryMessageSizeLimit() { return 64 * 1024; }
        @Override public List<WebSocketExtension> getExtensions() { return List.of(); }
        @Override public void sendMessage(WebSocketMessage<?> message) throws IOException { throw new IOException("simulated"); }
        @Override public boolean isOpen() { return open; }
        @Override public void close() { open = false; closed = true; }
        @Override public void close(CloseStatus status) { open = false; closed = true; }
    }
}

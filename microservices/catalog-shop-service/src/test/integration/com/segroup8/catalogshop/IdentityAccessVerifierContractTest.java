package com.segroup8.catalogshop;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.security.JwtPrincipal;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("CONTRACT")
class IdentityAccessVerifierContractTest {
    @Test
    void introspectionCarriesIdentityCorrelationAndIdempotencyHeaders() throws Exception {
        AtomicReference<String> requestId = new AtomicReference<>();
        AtomicReference<String> idempotencyKey = new AtomicReference<>();
        AtomicReference<String> token = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/auth/introspect", exchange -> {
            token.set(exchange.getRequestHeaders().getFirst("X-Internal-Service-Token"));
            requestId.set(exchange.getRequestHeaders().getFirst("X-Request-Id"));
            idempotencyKey.set(exchange.getRequestHeaders().getFirst("X-Idempotency-Key"));
            byte[] response = "{\"code\":0,\"data\":{\"active\":true,\"userId\":7,\"role\":\"USER\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var verifier = new IdentityAccessVerifier(new ObjectMapper(),
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/internal/auth/introspect",
                    "introspect", "service-token", 500, 500);
            verifier.requireActive("Bearer browser-token", new JwtPrincipal(7, "buyer", "USER"));
            assertThat(token.get()).isEqualTo("service-token");
            assertThat(requestId.get()).startsWith("catalog-introspect-");
            assertThat(idempotencyKey.get()).isEqualTo(requestId.get());
        } finally {
            server.stop(0);
        }
    }
}

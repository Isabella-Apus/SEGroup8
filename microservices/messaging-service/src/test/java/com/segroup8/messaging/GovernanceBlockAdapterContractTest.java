package com.segroup8.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.segroup8.messaging.access.MonolithGovernanceBlockAdapter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GovernanceBlockAdapterContractTest {
    @Test
    void boundedRetryReusesRequestAndIdempotencyHeaders() throws Exception {
        List<String> requestIds = new ArrayList<>();
        List<String> standardKeys = new ArrayList<>();
        List<String> compatibilityKeys = new ArrayList<>();
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/blocks/check", exchange -> {
            requestIds.add(exchange.getRequestHeaders().getFirst("X-Request-Id"));
            standardKeys.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            compatibilityKeys.add(exchange.getRequestHeaders().getFirst("X-Idempotency-Key"));
            exchange.getRequestBody().readAllBytes();
            if (attempts.incrementAndGet() == 1) {
                respond(exchange, 503, "{}");
            } else {
                respond(exchange, 200,
                        "{\"code\":0,\"data\":[{\"blockerId\":1,\"blockedId\":2,\"blocked\":false},"
                                + "{\"blockerId\":2,\"blockedId\":1,\"blocked\":false}]}");
            }
        });
        server.start();
        try {
            var adapter = new MonolithGovernanceBlockAdapter(RestClient.builder(),
                    "http://127.0.0.1:" + server.getAddress().getPort(), "contract-token",
                    500, 500, 2, 0);

            assertThat(adapter.isCommunicationBlocked(1, 2)).contains(false);
            assertThat(attempts).hasValue(2);
            assertThat(requestIds).doesNotContainNull().allMatch(requestIds.get(0)::equals);
            assertThat(standardKeys).doesNotContainNull().allMatch(standardKeys.get(0)::equals);
            assertThat(compatibilityKeys).containsExactlyElementsOf(standardKeys);
        } finally {
            server.stop(0);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}

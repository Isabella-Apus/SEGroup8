package com.segroup8.catalogshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
@Tag("DOMAIN_B")
class OutboxPublisherIntegrationTest {
    @Autowired JdbcClient db;
    @Autowired ObjectMapper json;

    @Test
    void sendsMessagingEnvelopeWithInternalToken() throws Exception {
        AtomicReference<String> token=new AtomicReference<>();
        AtomicReference<String> requestId=new AtomicReference<>();
        AtomicReference<String> standardIdempotency=new AtomicReference<>();
        AtomicReference<String> legacyIdempotency=new AtomicReference<>();
        AtomicReference<JsonNode> received=new AtomicReference<>();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/internal/events",exchange->{
            token.set(exchange.getRequestHeaders().getFirst("X-Internal-Service-Token"));
            requestId.set(exchange.getRequestHeaders().getFirst("X-Request-Id"));
            standardIdempotency.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            legacyIdempotency.set(exchange.getRequestHeaders().getFirst("X-Idempotency-Key"));
            received.set(json.readTree(exchange.getRequestBody()));
            exchange.sendResponseHeaders(204,-1);exchange.close();
        });
        server.start();
        try {
            insert("evt-envelope", "{\"recipientUserId\":7,\"displayTitle\":\"审核结果\",\"displayText\":\"已通过\",\"dedupeKey\":\"risk:1\"}");
            publisher(server).publishBatch();
            JsonNode envelope=received.get();
            assertNotNull(envelope);
            assertEquals("service-secret",token.get());
            assertEquals("evt-envelope",requestId.get());
            assertEquals("evt-envelope",standardIdempotency.get());
            assertEquals("evt-envelope",legacyIdempotency.get());
            assertEquals("evt-envelope",envelope.path("eventId").asText());
            assertEquals("NotificationRequested.v1",envelope.path("eventType").asText());
            assertEquals(1,envelope.path("eventVersion").asInt());
            assertEquals("catalog-shop-service",envelope.path("producer").asText());
            assertEquals(7,envelope.path("payload").path("recipientUserId").asInt());
            assertEquals("SENT",db.sql("select status from outbox_event where event_id='evt-envelope'").query(String.class).single());
        } finally { server.stop(0); }
    }

    @Test
    void failedDeliveryIsRetriedByOutbox() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/internal/events",exchange->{exchange.sendResponseHeaders(503,-1);exchange.close();});server.start();
        try {
            insert("evt-retry", "{\"orderId\":\"order-1\"}");
            publisher(server).publishBatch();
            var state=db.sql("select status,attempts,next_attempt_at,last_error from outbox_event where event_id='evt-retry'").query(RetryState.class).single();
            assertEquals("PENDING",state.status());assertEquals(1,state.attempts());assertNotNull(state.nextAttemptAt());assertTrue(state.lastError()!=null&&!state.lastError().isBlank());
        } finally { server.stop(0); }
    }

    private OutboxPublisher publisher(HttpServer server){return new OutboxPublisher(db,json,"http://127.0.0.1:"+server.getAddress().getPort(),"","service-secret",500,500);}
    private void insert(String eventId,String payload){db.sql("delete from outbox_event where event_id=:event").param("event",eventId).update();db.sql("insert into outbox_event(event_id,aggregate_type,aggregate_id,event_type,payload,status,destination,next_attempt_at) values(:event,'PRODUCT','1','NotificationRequested.v1',:payload,'PENDING','MESSAGING',CURRENT_TIMESTAMP)").params(java.util.Map.of("event",eventId,"payload",payload)).update();}
    record RetryState(String status,int attempts,java.time.Instant nextAttemptAt,String lastError){}
}

package com.segroup8.identity.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Publishes committed governance facts; consumers remain independently transactional. */
@Component
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final RestClient catalog;
    private final RestClient messaging;
    private final String internalToken;
    private final int maxAttempts;

    public OutboxPublisher(JdbcTemplate db, ObjectMapper json, RestClient.Builder builder,
            @Value("${app.outbox.catalog-url}") String catalogUrl,
            @Value("${app.outbox.messaging-url}") String messagingUrl,
            @Value("${app.internal-service-token}") String internalToken,
            @Value("${app.outbox.max-attempts:10}") int maxAttempts,
            @Value("${app.outbox.connect-timeout-ms:1500}") int connectTimeout,
            @Value("${app.outbox.read-timeout-ms:2500}") int readTimeout) {
        this.db = db;
        this.json = json;
        this.internalToken = internalToken;
        this.maxAttempts = Math.max(1, maxAttempts);
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeout));
        this.catalog = builder.clone().requestFactory(requestFactory).baseUrl(catalogUrl).build();
        this.messaging = builder.clone().requestFactory(requestFactory).baseUrl(messagingUrl).build();
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-ms:5000}")
    public void publishBatch() {
        pending().forEach(this::publish);
    }

    private List<Row> pending() {
        return db.query("select id,event_id,event_type,aggregate_type,aggregate_id,payload,retry_count,create_time "
                        + "from outbox_event where status='PENDING' and retry_count<? "
                        + "and (next_attempt_time is null or next_attempt_time<=current_timestamp) order by id limit 50",
                (rs, row) -> new Row(rs.getLong("id"), rs.getString("event_id"), rs.getString("event_type"),
                        rs.getString("aggregate_type"), rs.getLong("aggregate_id"), rs.getString("payload"),
                        rs.getInt("retry_count"), rs.getTimestamp("create_time").toInstant()), maxAttempts);
    }

    private void publish(Row event) {
        try {
            Map<String, Object> payload = json.readValue(event.payload(), new TypeReference<>() {});
            if ("MerchantApproved.v1".equals(event.eventType())) {
                publishMerchantToCatalog(event, payload);
                payload = merchantNotification(payload, event);
            } else if (!"UserAccessChanged.v1".equals(event.eventType())) {
                throw new IllegalArgumentException("Unsupported identity event " + event.eventType());
            }
            publishEnvelopeToMessaging(event, payload);
            db.update("update outbox_event set status='PUBLISHED',published_time=current_timestamp,"
                    + "last_error=null where id=? and status='PENDING'", event.id());
        } catch (Exception failure) {
            int nextAttempt = event.retryCount() + 1;
            long delaySeconds = Math.min(300, 1L << Math.min(nextAttempt, 8));
            String message = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            if (message.length() > 500) message = message.substring(0, 500);
            db.update("update outbox_event set retry_count=?,status=?,next_attempt_time=?,last_error=? "
                            + "where id=? and status='PENDING'",
                    nextAttempt, nextAttempt >= maxAttempts ? "DEAD" : "PENDING",
                    Timestamp.from(Instant.now().plusSeconds(delaySeconds)), message, event.id());
            log.warn("identity outbox delivery failed eventId={} eventType={} attempt={} error={}",
                    event.eventId(), event.eventType(), nextAttempt, failure.getClass().getSimpleName());
        }
    }

    private void publishMerchantToCatalog(Row event, Map<String, Object> payload) {
        catalog.post().uri("/internal/events/merchant-approved")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Service-Token", internalToken)
                .header("X-Request-Id", event.eventId())
                .header("Idempotency-Key", event.eventId())
                .header("X-Idempotency-Key", event.eventId())
                .body(Map.of("eventId", event.eventId(),
                        "applicationId", String.valueOf(payload.get("applicationId")),
                        "sellerId", number(payload.get("userId")),
                        "shopName", String.valueOf(payload.get("storeName"))))
                .retrieve().toBodilessEntity();
    }

    private Map<String, Object> merchantNotification(Map<String, Object> source, Row event) {
        Map<String, Object> payload = new LinkedHashMap<>(source);
        payload.put("recipientUserId", number(source.get("userId")));
        payload.put("displayTitle", "Merchant application approved");
        payload.put("displayText", "Your merchant application has been approved");
        payload.put("targetPath", "/merchant/home");
        payload.put("businessType", "MERCHANT_APPLICATION");
        payload.put("businessId", String.valueOf(source.get("applicationId")));
        payload.put("dedupeKey", event.eventId() + ":notification");
        return payload;
    }

    private void publishEnvelopeToMessaging(Row event, Map<String, Object> payload) {
        messaging.post().uri("/internal/events")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Service-Token", internalToken)
                .header("X-Request-Id", event.eventId())
                .header("Idempotency-Key", event.eventId())
                .header("X-Idempotency-Key", event.eventId())
                .body(Map.of("eventId", event.eventId(), "eventType", event.eventType(), "eventVersion", 1,
                        "producer", "identity-governance-service", "aggregateType", event.aggregateType(),
                        "aggregateId", Long.toString(event.aggregateId()), "occurredAt", event.createdAt(),
                        "traceId", event.eventId(), "payload", payload))
                .retrieve().toBodilessEntity();
    }

    private long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private record Row(long id, String eventId, String eventType, String aggregateType,
            long aggregateId, String payload, int retryCount, Instant createdAt) {}
}

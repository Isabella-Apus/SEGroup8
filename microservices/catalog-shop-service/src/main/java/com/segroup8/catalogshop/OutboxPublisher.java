package com.segroup8.catalogshop;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class OutboxPublisher {
    private static final int MAX_ATTEMPTS = 10;
    private final JdbcClient db;
    private final RestClient client;
    private final boolean enabled;

    OutboxPublisher(JdbcClient db, RestClient.Builder builder,
            @Value("${catalog-shop.outbox-target-url:}") String targetUrl) {
        this.db = db;
        this.enabled = targetUrl != null && !targetUrl.isBlank();
        this.client = enabled ? builder.baseUrl(targetUrl).build() : null;
    }

    @Scheduled(fixedDelayString = "${catalog-shop.outbox-publish-ms:5000}")
    void publishBatch() {
        if (!enabled) return;
        pending().forEach(this::publish);
    }

    private List<OutboxRow> pending() {
        return db.sql("select id,event_id,event_type,payload,attempts from outbox_event "
                + "where status='PENDING' and attempts<:max order by id limit 50")
                .param("max", MAX_ATTEMPTS).query(OutboxRow.class).list();
    }

    private void publish(OutboxRow event) {
        try {
            client.post().uri("/internal/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Event-Id", event.eventId())
                    .header("X-Event-Type", event.eventType())
                    .body(event.payload()).retrieve().toBodilessEntity();
            db.sql("update outbox_event set status='SENT' where id=:id and status='PENDING'")
                    .param("id", event.id()).update();
        } catch (RuntimeException failure) {
            db.sql("update outbox_event set attempts=attempts+1,status=case when attempts+1>=:max "
                    + "then 'DEAD' else 'PENDING' end where id=:id and status='PENDING'")
                    .params(Map.of("max", MAX_ATTEMPTS, "id", event.id())).update();
        }
    }
}

record OutboxRow(long id, String eventId, String eventType, String payload, int attempts) {}

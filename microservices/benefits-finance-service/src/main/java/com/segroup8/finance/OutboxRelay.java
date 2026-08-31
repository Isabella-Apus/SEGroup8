package com.segroup8.finance;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class OutboxRelay {
    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);
    private final JdbcClient db;
    private final RestClient http;
    private final String sinkUrl;
    private final String serviceToken;
    private final int batchSize;
    private final int maxAttempts;

    OutboxRelay(JdbcClient db, @Qualifier("outboxRestClient") RestClient http,
            @Value("${app.outbox-event-sink-url:}") String sinkUrl,
            @Value("${app.internal-service-token}") String serviceToken,
            @Value("${app.outbox-batch-size:50}") int batchSize,
            @Value("${app.outbox-max-attempts:8}") int maxAttempts) {
        this.db = db;
        this.http = http;
        this.sinkUrl = sinkUrl;
        this.serviceToken = serviceToken;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString="${app.outbox-poll-ms:5000}")
    public void publishPending() {
        if (sinkUrl == null || sinkUrl.isBlank()) return;
        Instant now = Instant.now();
        Instant stale = now.minus(5, ChronoUnit.MINUTES);
        List<Event> events = db.sql("select event_id,event_type,payload,attempts from outbox_event "
                        + "where available_at<=:now and (status='PENDING' or (status='SENDING' and locked_at<:stale)) "
                        + "order by created_at limit :batch")
                .param("now", now).param("stale", stale).param("batch", batchSize)
                .query((rs, row) -> new Event(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4))).list();
        for (Event event : events) publish(event, now, stale);
    }

    private void publish(Event event, Instant now, Instant stale) {
        int claimed = db.sql("update outbox_event set status='SENDING',locked_at=:now where event_id=:id "
                        + "and (status='PENDING' or (status='SENDING' and locked_at<:stale))")
                .param("now", now).param("id", event.id()).param("stale", stale).update();
        if (claimed == 0) return;
        try {
            http.post().uri(sinkUrl).contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Service-Token", serviceToken)
                    .header("X-Event-Id", event.id()).header("X-Event-Type", event.type())
                    .body(event.payload()).retrieve().toBodilessEntity();
            db.sql("update outbox_event set status='PUBLISHED',published_at=current_timestamp,locked_at=null "
                            + "where event_id=:id and status='SENDING'")
                    .param("id", event.id()).update();
            LOG.info("outbox_event_published eventId={} eventType={}", event.id(), event.type());
        } catch (RuntimeException failure) {
            int nextAttempt = event.attempts() + 1;
            if (nextAttempt >= maxAttempts) {
                db.sql("update outbox_event set status='DEAD',attempts=attempts+1,locked_at=null "
                                + "where event_id=:id and status='SENDING'")
                        .param("id", event.id()).update();
                LOG.error("outbox_event_dead_lettered eventId={} eventType={} attempt={}",
                        event.id(), event.type(), nextAttempt);
                return;
            }
            long delay = Math.min(300, 1L << Math.min(event.attempts(), 8));
            db.sql("update outbox_event set status='PENDING',attempts=attempts+1,available_at=:available,locked_at=null "
                            + "where event_id=:id and status='SENDING'")
                    .param("available", Instant.now().plus(delay, ChronoUnit.SECONDS)).param("id", event.id()).update();
            LOG.warn("outbox_event_retry_scheduled eventId={} eventType={} attempt={}",
                    event.id(), event.type(), nextAttempt);
        }
    }

    private record Event(String id, String type, String payload, int attempts) {}
}

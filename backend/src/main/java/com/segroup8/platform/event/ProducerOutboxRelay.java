package com.segroup8.platform.event;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProducerOutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(ProducerOutboxRelay.class);
    private final JdbcTemplate jdbc;
    private final RestClient client;
    private final String serviceToken;
    private final int maxRetries;

    public ProducerOutboxRelay(JdbcTemplate jdbc, RestClient.Builder builder,
            @Value("${app.messaging.base-url:http://127.0.0.1:8084}") String baseUrl,
            @Value("${app.internal-service.token:}") String serviceToken,
            @Value("${app.outbox.max-retries:20}") int maxRetries) {
        this.jdbc = jdbc;
        this.client = builder.baseUrl(baseUrl).build();
        this.serviceToken = serviceToken;
        this.maxRetries = maxRetries;
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay-delay-ms:1000}")
    public void relay() {
        if (serviceToken == null || serviceToken.isBlank()) return;
        List<Row> rows = jdbc.query("select id,event_id,payload,retry_count from outbox_event " +
                        "where status in ('PENDING','RETRY') and next_attempt_at<=current_timestamp " +
                        "order by id limit 50",
                (rs, n) -> new Row(rs.getLong("id"), rs.getString("event_id"),
                        rs.getString("payload"), rs.getInt("retry_count")));
        rows.forEach(this::deliver);
    }

    private void deliver(Row row) {
        try {
            client.post().uri("/internal/events")
                    .header("X-Internal-Service-Token", serviceToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(row.payload()).retrieve().toBodilessEntity();
            jdbc.update("update outbox_event set status='PUBLISHED',published_at=current_timestamp,last_error=null where id=?", row.id());
        } catch (RuntimeException ex) {
            int attempts = row.retryCount() + 1;
            String status = attempts >= maxRetries ? "DLQ" : "RETRY";
            long delaySeconds = Math.min(300, 1L << Math.min(attempts, 8));
            jdbc.update("update outbox_event set status=?,retry_count=?,next_attempt_at=?,last_error=? where id=?",
                    status, attempts, LocalDateTime.now().plusSeconds(delaySeconds), compact(ex.getMessage()), row.id());
            log.warn("Producer outbox delivery failed eventId={} attempt={} status={}", row.eventId(), attempts, status);
        }
    }

    private String compact(String value) {
        if (value == null) return "delivery failed";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private record Row(long id, String eventId, String payload, int retryCount) { }
}

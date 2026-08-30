package com.segroup8.messaging.delivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.messaging.realtime.RealtimePublisher;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeliveryWorker {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final RealtimePublisher realtime;
    private final int maxRetries;

    public DeliveryWorker(JdbcTemplate jdbc, ObjectMapper json, RealtimePublisher realtime,
            @Value("${app.delivery.max-retries:5}") int maxRetries) {
        this.jdbc = jdbc; this.json = json; this.realtime = realtime; this.maxRetries = maxRetries;
    }

    @Scheduled(fixedDelayString = "${app.delivery.poll-delay-ms:1000}")
    public void runOnce() {
        List<Row> rows = jdbc.query("select id,recipient_user_id,event_type,payload,retry_count,delivery_kind " +
                        "from outbox_event where status in ('PENDING','RETRY') and next_attempt_at<=current_timestamp " +
                        "order by id limit 50",
                (rs, n) -> new Row(rs.getLong("id"), (Long) rs.getObject("recipient_user_id"),
                        rs.getString("event_type"), rs.getString("payload"), rs.getInt("retry_count"),
                        rs.getString("delivery_kind")));
        rows.forEach(this::deliver);
    }

    private void deliver(Row row) {
        if (DeliveryOutboxService.AUDIT.equals(row.kind())) {
            jdbc.update("update outbox_event set next_attempt_at=?,last_error='AUDIT_SINK_UNAVAILABLE' where id=?",
                    LocalDateTime.now().plusMinutes(5), row.id());
            return;
        }
        if (row.recipient() == null || realtime.sessionCount(row.recipient()) == 0) {
            jdbc.update("update outbox_event set next_attempt_at=?,last_error='RECIPIENT_OFFLINE' where id=?",
                    LocalDateTime.now().plusSeconds(30), row.id());
            return;
        }
        try {
            JsonNode payload = json.readTree(row.payload());
            if (!realtime.tryPushToUser(row.recipient(), row.eventType(), payload)) {
                throw new IllegalStateException("No WebSocket session accepted delivery");
            }
            jdbc.update("update outbox_event set status='DELIVERED',delivered_at=current_timestamp,last_error=null where id=?", row.id());
        } catch (Exception ex) {
            int attempts = row.retryCount() + 1;
            String status = attempts >= maxRetries ? "DLQ" : "RETRY";
            jdbc.update("update outbox_event set status=?,retry_count=?,next_attempt_at=?,last_error=? where id=?",
                    status, attempts, LocalDateTime.now().plusSeconds(Math.min(300, 1L << attempts)),
                    compact(ex.getMessage()), row.id());
        }
    }

    private String compact(String value) {
        if (value == null) return "delivery failed";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
    private record Row(long id, Long recipient, String eventType, String payload, int retryCount, String kind) { }
}

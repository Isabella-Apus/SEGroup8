package com.segroup8.messaging.event;

import com.segroup8.messaging.common.MessagingMetrics;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InboxWorker {
    private static final Logger log = LoggerFactory.getLogger(InboxWorker.class);
    private final JdbcTemplate jdbc;
    private final InboxEventService inbox;
    private final int maxRetries;
    private final MessagingMetrics metrics;
    public InboxWorker(JdbcTemplate jdbc, InboxEventService inbox,
            @Value("${app.inbox.max-retries:5}") int maxRetries, MessagingMetrics metrics) {
        this.jdbc = jdbc; this.inbox = inbox; this.maxRetries = maxRetries; this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${app.inbox.poll-delay-ms:1000}")
    public void runOnce() {
        Integer backlog = jdbc.queryForObject("select count(*) from inbox_event where status in ('RECEIVED','RETRY','DLQ')", Integer.class);
        metrics.setEventBacklog(backlog == null ? 0 : backlog);
        List<Row> rows = jdbc.query("select id,event_id,event_type,trace_id,retry_count from inbox_event where status in ('RECEIVED','RETRY') " +
                        "and next_retry_at<=current_timestamp order by id limit 50",
                (rs, n) -> new Row(rs.getLong("id"), rs.getString("event_id"), rs.getString("event_type"), rs.getString("trace_id"), rs.getInt("retry_count")));
        for (Row row : rows) {
            try { log.info("messaging inbox process eventId={} eventType={} traceId={}", row.eventId(), row.eventType(), row.traceId()); inbox.process(row.id()); }
            catch (RuntimeException ex) { metrics.eventConsumeFailed(); metrics.retried(); log.warn("messaging inbox failure eventId={} eventType={} traceId={} retryCount={} error={}", row.eventId(), row.eventType(), row.traceId(), row.retries() + 1, ex.getClass().getSimpleName()); inbox.recordFailure(row.id(), row.retries(), maxRetries, ex); }
        }
    }
    private record Row(long id, String eventId, String eventType, String traceId, int retries) { }
}

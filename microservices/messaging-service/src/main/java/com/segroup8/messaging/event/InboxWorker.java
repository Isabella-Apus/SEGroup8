package com.segroup8.messaging.event;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InboxWorker {
    private final JdbcTemplate jdbc;
    private final InboxEventService inbox;
    private final int maxRetries;
    public InboxWorker(JdbcTemplate jdbc, InboxEventService inbox,
            @Value("${app.inbox.max-retries:5}") int maxRetries) {
        this.jdbc = jdbc; this.inbox = inbox; this.maxRetries = maxRetries;
    }

    @Scheduled(fixedDelayString = "${app.inbox.poll-delay-ms:1000}")
    public void runOnce() {
        List<Row> rows = jdbc.query("select id,retry_count from inbox_event where status in ('RECEIVED','RETRY') " +
                        "and next_retry_at<=current_timestamp order by id limit 50",
                (rs, n) -> new Row(rs.getLong(1), rs.getInt(2)));
        for (Row row : rows) {
            try { inbox.process(row.id()); }
            catch (RuntimeException ex) { inbox.recordFailure(row.id(), row.retries(), maxRetries, ex); }
        }
    }
    private record Row(long id, int retries) { }
}

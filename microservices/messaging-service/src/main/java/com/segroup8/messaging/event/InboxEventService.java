package com.segroup8.messaging.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.messaging.common.ApiException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboxEventService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final EventHandler handler;

    public InboxEventService(JdbcTemplate jdbc, ObjectMapper json, EventHandler handler) {
        this.jdbc = jdbc; this.json = json; this.handler = handler;
    }

    @Transactional
    public boolean accept(EventEnvelope event) {
        try {
            jdbc.update("insert into inbox_event(event_id,event_type,payload,status,retry_count,next_retry_at," +
                            "received_at,trace_id) values(?,?,?,'RECEIVED',0,current_timestamp,current_timestamp,?)",
                    event.eventId(), event.eventType(), serialize(event), event.traceId());
            return true;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    @Transactional
    public void process(long id) {
        List<String> rows = jdbc.query("select payload from inbox_event where id=? and status in ('RECEIVED','RETRY')",
                (rs, n) -> rs.getString(1), id);
        if (rows.isEmpty()) return;
        EventEnvelope event = deserialize(rows.get(0));
        handler.handle(event);
        jdbc.update("update inbox_event set status='PROCESSED',processed_at=current_timestamp,last_error=null where id=?", id);
    }

    @Transactional
    public void replay(String eventId, String operator, String reason, String traceId,
            com.segroup8.messaging.delivery.DeliveryOutboxService delivery) {
        Integer count = jdbc.queryForObject("select count(*) from inbox_event where event_id=?", Integer.class, eventId);
        if (count == null || count == 0) throw new ApiException(404, "Event not found");
        String auditDedupe = "audit:replay:" + eventId + ":" + java.util.UUID.randomUUID();
        delivery.enqueueAudit(eventId, auditDedupe, java.util.Map.of(
                "operator", operator, "eventId", eventId, "action", "REPLAY",
                "reason", reason == null ? "not supplied" : reason,
                "traceId", traceId, "timestamp", java.time.Instant.now().toString(), "result", "ACCEPTED"), traceId);
        jdbc.update("update inbox_event set status='RECEIVED',retry_count=0,next_retry_at=current_timestamp," +
                "last_error=null,processed_at=null where event_id=?", eventId);
    }

    public void recordFailure(long id, int previousRetries, int maxRetries, Throwable error) {
        int attempts = previousRetries + 1;
        String status = attempts >= maxRetries ? "DLQ" : "RETRY";
        jdbc.update("update inbox_event set status=?,retry_count=?,next_retry_at=?,last_error=? where id=?",
                status, attempts, LocalDateTime.now().plusSeconds(Math.min(300, 1L << attempts)),
                compact(error.getMessage()), id);
    }

    private String serialize(EventEnvelope event) {
        try { return json.writeValueAsString(event); }
        catch (JsonProcessingException ex) { throw new ApiException(400, "Invalid event payload"); }
    }
    private EventEnvelope deserialize(String value) {
        try { return json.readValue(value, EventEnvelope.class); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("Stored event cannot be parsed", ex); }
    }
    private String compact(String value) {
        if (value == null) return "processing failed";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}

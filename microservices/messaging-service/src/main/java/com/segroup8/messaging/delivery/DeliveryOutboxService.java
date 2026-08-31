package com.segroup8.messaging.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DeliveryOutboxService {
    public static final String WEBSOCKET = "WEBSOCKET";
    public static final String AUDIT = "AUDIT";
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public DeliveryOutboxService(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc; this.json = json;
    }

    public void enqueueWebSocket(String sourceEventId, String dedupeKey, long recipientUserId,
            String eventType, Object payload, String traceId) {
        insert(UUID.randomUUID().toString(), sourceEventId, dedupeKey, WEBSOCKET,
                recipientUserId, eventType, payload, traceId);
    }

    public void enqueueAudit(String sourceEventId, String dedupeKey, Object payload, String traceId) {
        insert(UUID.randomUUID().toString(), sourceEventId, dedupeKey, AUDIT,
                null, "REPLAY_AUDIT", payload, traceId);
    }

    private void insert(String eventId, String sourceEventId, String dedupeKey, String kind,
            Long recipientUserId, String eventType, Object payload, String traceId) {
        try {
            jdbc.update("insert into outbox_event(event_id,source_event_id,dedupe_key,delivery_kind," +
                            "recipient_user_id,event_type,payload,trace_id,status,retry_count,next_attempt_at,created_at) " +
                            "values(?,?,?,?,?,?,?,?,'PENDING',0,current_timestamp,current_timestamp)",
                    eventId, sourceEventId, dedupeKey, kind, recipientUserId, eventType,
                    serialize(payload), traceId == null || traceId.isBlank() ? eventId : traceId);
        } catch (DuplicateKeyException ignored) {
            // The durable dedupe boundary already owns this delivery.
        }
    }

    private String serialize(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("Delivery payload cannot be serialized", ex); }
    }
}

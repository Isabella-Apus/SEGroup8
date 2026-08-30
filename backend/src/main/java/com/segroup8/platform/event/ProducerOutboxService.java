package com.segroup8.platform.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ProducerOutboxService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public ProducerOutboxService(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public String publish(String eventType, String producer, String aggregateType,
            Object aggregateId, Map<String, Object> payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Producer outbox write must run inside the business transaction");
        }
        String eventId = UUID.randomUUID().toString();
        String traceId = TraceContext.currentTraceId();
        EventEnvelope envelope = new EventEnvelope(eventId, eventType, 1, producer,
                aggregateType, String.valueOf(aggregateId), Instant.now(), traceId, payload);
        jdbc.update("insert into outbox_event(event_id,event_type,aggregate_type,aggregate_id,payload,trace_id," +
                        "status,retry_count,next_attempt_at,created_at) values(?,?,?,?,?,?,'PENDING',0,current_timestamp,current_timestamp)",
                eventId, eventType, aggregateType, String.valueOf(aggregateId), serialize(envelope), traceId);
        return eventId;
    }

    public String notification(String producer, String aggregateType, Object aggregateId,
            List<Long> recipientUserIds, String title, String content, String targetPath,
            String notificationType, String businessType, String dedupeKey) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientUserIds", recipientUserIds);
        payload.put("businessId", String.valueOf(aggregateId));
        payload.put("sourceId", String.valueOf(aggregateId));
        payload.put("sourceType", businessType);
        payload.put("displayTitle", title == null || title.isBlank() ? "[Notification]" : title);
        payload.put("displayText", content == null || content.isBlank() ? "[Details unavailable]" : content);
        payload.put("targetPath", targetPath);
        payload.put("notificationType", notificationType);
        payload.put("businessType", businessType);
        payload.put("dedupeKey", dedupeKey);
        return publish(EventTypes.NOTIFICATION_REQUESTED, producer, aggregateType, aggregateId, payload);
    }

    private String serialize(EventEnvelope envelope) {
        try {
            return json.writeValueAsString(envelope);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Event envelope cannot be serialized", ex);
        }
    }
}

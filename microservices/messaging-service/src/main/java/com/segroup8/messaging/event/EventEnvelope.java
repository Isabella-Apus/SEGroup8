package com.segroup8.messaging.event;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record EventEnvelope(String eventId, String eventType, int eventVersion,
        String producer, String aggregateType, String aggregateId,
        Instant occurredAt, String traceId, Map<String, Object> payload) {
    public EventEnvelope {
        required(eventId, "eventId");
        if (!EventTypes.ALL.contains(eventType)) throw new IllegalArgumentException("Unsupported eventType: " + eventType);
        if (eventVersion != 1) throw new IllegalArgumentException("eventVersion must be 1");
        required(producer, "producer"); required(aggregateType, "aggregateType");
        required(aggregateId, "aggregateId"); required(traceId, "traceId");
        if (occurredAt == null) throw new IllegalArgumentException("occurredAt is required");
        payload = payload == null ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }
    private static void required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}

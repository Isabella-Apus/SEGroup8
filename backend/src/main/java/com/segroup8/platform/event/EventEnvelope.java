package com.segroup8.platform.event;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record EventEnvelope(
        String eventId,
        String eventType,
        int eventVersion,
        String producer,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        String traceId,
        Map<String, Object> payload) {

    public EventEnvelope {
        if (eventId == null || eventId.isBlank()) throw new IllegalArgumentException("eventId is required");
        if (!EventTypes.ALL.contains(eventType)) throw new IllegalArgumentException("Unsupported eventType: " + eventType);
        if (eventVersion != 1) throw new IllegalArgumentException("eventVersion must be 1");
        if (producer == null || producer.isBlank()) throw new IllegalArgumentException("producer is required");
        if (aggregateType == null || aggregateType.isBlank()) throw new IllegalArgumentException("aggregateType is required");
        if (aggregateId == null || aggregateId.isBlank()) throw new IllegalArgumentException("aggregateId is required");
        if (occurredAt == null) throw new IllegalArgumentException("occurredAt is required");
        if (traceId == null || traceId.isBlank()) throw new IllegalArgumentException("traceId is required");
        payload = payload == null ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }
}

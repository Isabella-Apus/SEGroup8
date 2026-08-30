package com.segroup8.secondhand.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxRepository {
    private static final Logger log = LoggerFactory.getLogger(OutboxRepository.class);
    private final NamedParameterJdbcTemplate db;
    private final ObjectMapper objectMapper;

    public OutboxRepository(NamedParameterJdbcTemplate db, ObjectMapper objectMapper) {
        this.db = db;
        this.objectMapper = objectMapper;
    }

    public String append(String aggregateType, Object aggregateId, String eventType, Map<String, ?> payload) {
        String eventId = UUID.randomUUID().toString();
        db.update("insert into outbox_event(event_id,aggregate_type,aggregate_id,event_type,payload) "
                        + "values(:eventId,:aggregateType,:aggregateId,:eventType,:payload)",
                new MapSqlParameterSource().addValue("eventId", eventId).addValue("aggregateType", aggregateType)
                        .addValue("aggregateId", String.valueOf(aggregateId)).addValue("eventType", eventType)
                        .addValue("payload", json(payload)));
        log.info("outbox event stored eventId={} eventType={} aggregateType={} aggregateId={}",
                eventId, eventType, aggregateType, aggregateId);
        return eventId;
    }

    public long countByType(String eventType) {
        Long count = db.queryForObject("select count(*) from outbox_event where event_type=:type",
                Map.of("type", eventType), Long.class);
        return count == null ? 0 : count;
    }

    private String json(Map<String, ?> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot serialize outbox payload", exception);
        }
    }
}

package com.segroup8.secondhand.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.time.Instant;
import java.util.List;
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

    public List<OutboxRow> pendingDeliveries() {
        return db.query("select id,event_id,aggregate_type,aggregate_id,event_type,payload,attempts,create_time "
                        + "from outbox_event where event_status='NEW' and available_at<=current_timestamp "
                        + "and event_type in ('NotificationRequested.v1','SecondhandTradeSettled.v1') "
                        + "order by id limit 50", Map.of(),
                (rs,row)->new OutboxRow(rs.getLong("id"),rs.getString("event_id"),
                        rs.getString("aggregate_type"),rs.getString("aggregate_id"),rs.getString("event_type"),
                        rs.getString("payload"),rs.getInt("attempts"),rs.getTimestamp("create_time").toInstant()));
    }

    public void markPublished(long id) {
        db.update("update outbox_event set event_status='PUBLISHED',published_at=current_timestamp "
                + "where id=:id and event_status='NEW'",Map.of("id",id));
    }

    public void markRetry(long id,int attempts) {
        db.update("update outbox_event set attempts=attempts+1,available_at=:next where id=:id and event_status='NEW'",
                Map.of("id",id,"next",java.sql.Timestamp.from(Instant.now().plusSeconds(Math.min(300,1L<<Math.min(attempts+1,8))))));
    }

    public record OutboxRow(long id,String eventId,String aggregateType,String aggregateId,
            String eventType,String payload,int attempts,Instant createdAt) {}

    private String json(Map<String, ?> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot serialize outbox payload", exception);
        }
    }
}

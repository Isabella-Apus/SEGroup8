package com.segroup8.catalogshop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class OutboxPublisher {
    private static final int MAX_ATTEMPTS=10;
    private final JdbcClient db;private final ObjectMapper json;private final RestClient client;
    private final String messagingUrl;private final String orderUrl;private final String internalToken;
    OutboxPublisher(JdbcClient db,ObjectMapper json,
            @Value("${catalog-shop.messaging-event-url:}") String messagingUrl,
            @Value("${catalog-shop.order-event-url:}") String orderUrl,
            @Value("${catalog-shop.internal-token}") String internalToken,
            @Value("${catalog-shop.http.connect-timeout-ms:1500}") int connectTimeout,
            @Value("${catalog-shop.http.read-timeout-ms:2500}") int readTimeout){
        this.db=db;this.json=json;this.messagingUrl=messagingUrl;this.orderUrl=orderUrl;this.internalToken=internalToken;
        var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(Duration.ofMillis(connectTimeout));factory.setReadTimeout(Duration.ofMillis(readTimeout));this.client=RestClient.builder().requestFactory(factory).build();
    }
    @Scheduled(fixedDelayString="${catalog-shop.outbox-publish-ms:5000}") void publishBatch(){pending().forEach(this::publish);}
    private List<OutboxRow> pending(){return db.sql("select id,event_id,event_type,aggregate_type,aggregate_id,payload,destination,attempts,created_at from outbox_event where status='PENDING' and attempts<:max and (next_attempt_at is null or next_attempt_at<=CURRENT_TIMESTAMP) order by id limit 50").param("max",MAX_ATTEMPTS).query(OutboxRow.class).list();}
    private void publish(OutboxRow event){String base="MESSAGING".equals(event.destination())?messagingUrl:orderUrl;if(base==null||base.isBlank())return;String trace=event.eventId();try{MDC.put("eventId",event.eventId());Map<String,Object> payload=json.readValue(event.payload(),new TypeReference<>(){});Map<String,Object> envelope=new LinkedHashMap<>();envelope.put("eventId",event.eventId());envelope.put("eventType",event.eventType());envelope.put("eventVersion",1);envelope.put("producer","catalog-shop-service");envelope.put("aggregateType",event.aggregateType());envelope.put("aggregateId",event.aggregateId());envelope.put("occurredAt",event.createdAt());envelope.put("traceId",trace);envelope.put("payload",payload);client.post().uri(base+"/internal/events").contentType(MediaType.APPLICATION_JSON).header("X-Internal-Service-Token",internalToken).header("X-Trace-Id",trace).body(envelope).retrieve().toBodilessEntity();db.sql("update outbox_event set status='SENT',sent_at=CURRENT_TIMESTAMP,last_error=null where id=:id and status='PENDING'").param("id",event.id()).update();}catch(Exception failure){long seconds=Math.min(300,1L<<Math.min(event.attempts()+1,8));String message=failure.getMessage()==null?failure.getClass().getSimpleName():failure.getMessage();if(message.length()>500)message=message.substring(0,500);db.sql("update outbox_event set attempts=attempts+1,status=case when attempts+1>=:max then 'DEAD' else 'PENDING' end,next_attempt_at=:next,last_error=:error where id=:id and status='PENDING'").params(Map.of("max",MAX_ATTEMPTS,"next",java.sql.Timestamp.from(Instant.now().plusSeconds(seconds)),"error",message,"id",event.id())).update();}finally{MDC.remove("eventId");}}
}

record OutboxRow(long id,String eventId,String eventType,String aggregateType,String aggregateId,String payload,String destination,int attempts,Instant createdAt){}

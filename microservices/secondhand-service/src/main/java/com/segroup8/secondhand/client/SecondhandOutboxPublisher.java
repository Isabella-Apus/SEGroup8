package com.segroup8.secondhand.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.secondhand.repository.OutboxRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SecondhandOutboxPublisher {
    private final OutboxRepository outbox;
    private final ObjectMapper json;
    private final RestClient messaging;
    private final String token;

    public SecondhandOutboxPublisher(OutboxRepository outbox,ObjectMapper json,RestClient.Builder builder,
            @Value("${clients.messaging.base-url}") String messagingUrl,
            @Value("${security.internal-token}") String token) {
        this.outbox=outbox;this.json=json;this.messaging=builder.baseUrl(messagingUrl).build();this.token=token;
    }

    @Scheduled(fixedDelayString="${clients.messaging.publish-delay-ms:2000}")
    public void publish() {
        for(var event:outbox.pendingDeliveries()) {
            try {
                Map<String,Object> payload=new LinkedHashMap<>(json.readValue(event.payload(),new TypeReference<>(){}));
                normalize(event,payload);
                Map<String,Object> envelope=Map.of("eventId",event.eventId(),"eventType",event.eventType(),
                        "eventVersion",1,"producer","secondhand-service","aggregateType",event.aggregateType(),
                        "aggregateId",event.aggregateId(),"occurredAt",event.createdAt(),"traceId",event.eventId(),
                        "payload",payload);
                messaging.post().uri("/internal/events").header("X-Internal-Service-Token",token)
                        .header("X-Request-Id",event.eventId()).header("X-Idempotency-Key",event.eventId())
                        .header("Idempotency-Key",event.eventId()).body(envelope).retrieve().toBodilessEntity();
                outbox.markPublished(event.id());
            } catch(Exception failure) {
                outbox.markRetry(event.id(),event.attempts());
            }
        }
    }

    private void normalize(OutboxRepository.OutboxRow event,Map<String,Object> payload) {
        Object recipients=payload.remove("recipientIds");
        if(recipients!=null) payload.put("recipientUserIds",recipients);
        if("SecondhandTradeSettled.v1".equals(event.eventType())) {
            payload.put("recipientUserIds",List.of(payload.get("buyerId"),payload.get("sellerId")));
        }
        payload.putIfAbsent("displayTitle","Secondhand trade update");
        payload.putIfAbsent("displayText","Open the secondhand order to view the latest status");
        payload.putIfAbsent("targetPath","/user/orders");
        payload.putIfAbsent("businessType","SECONDHAND_TRADE");
        payload.putIfAbsent("businessId",event.aggregateId());
        payload.putIfAbsent("dedupeKey",event.eventId());
    }
}

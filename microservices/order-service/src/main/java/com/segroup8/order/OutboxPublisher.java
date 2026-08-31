package com.segroup8.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name="outbox.publisher.enabled",havingValue="true",matchIfMissing=true)
class OutboxPublisher {
    private final OrderRepository repository;
    private final RestClient messaging;
    private final String internalToken;
    private final ObjectMapper json;

    OutboxPublisher(OrderRepository repository,RestClient.Builder builder,ObjectMapper json,
            @Value("${downstream.messaging-url}") String messagingUrl,
            @Value("${security.internal-service-token}") String internalToken) {
        this.repository=repository;this.messaging=builder.baseUrl(messagingUrl).build();
        this.internalToken=internalToken;this.json=json;
    }

    @Scheduled(fixedDelayString="${outbox.publisher.fixed-delay:2s}")
    void publish() {
        for (var event:repository.pendingOutbox()) {
            try {
                messaging.post().uri("/internal/events").header("X-Internal-Service-Token",internalToken)
                        .header("Idempotency-Key",event.eventId())
                        .header("X-Idempotency-Key",event.eventId())
                        .header("X-Request-Id",event.eventId())
                        .body(Map.of("eventId",event.eventId(),"eventType",event.eventType(),"eventVersion",1,
                                "producer","order-service","aggregateType",event.aggregateType(),
                                "aggregateId",event.aggregateId(),"occurredAt",event.createdAt(),
                                "traceId",event.eventId(),"payload",json.readTree(event.payload())))
                        .retrieve().toBodilessEntity();
                repository.markPublished(event.eventId());
            } catch (Exception ex) {
                repository.markOutboxRetry(event.eventId());
            }
        }
    }
}

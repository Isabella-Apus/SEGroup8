package com.segroup8.order;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name="outbox.publisher.enabled",havingValue="true",matchIfMissing=true)
class OutboxPublisher {
    private final OrderRepository repository;
    private final RestClient messaging;
    private final String internalToken;

    OutboxPublisher(OrderRepository repository,RestClient.Builder builder,
            @Value("${downstream.messaging-url}") String messagingUrl,
            @Value("${security.internal-service-token}") String internalToken) {
        this.repository=repository;this.messaging=builder.baseUrl(messagingUrl).build();this.internalToken=internalToken;
    }

    @Scheduled(fixedDelayString="${outbox.publisher.fixed-delay:2s}")
    void publish() {
        for (var event:repository.pendingOutbox()) {
            try {
                messaging.post().uri("/internal/events").header("X-Internal-Service-Token",internalToken)
                        .header("Idempotency-Key",event.eventId())
                        .body(Map.of("eventId",event.eventId(),"eventType",event.eventType(),"payload",event.payload()))
                        .retrieve().toBodilessEntity();
                repository.markPublished(event.eventId());
            } catch (RestClientException ex) {
                repository.markOutboxRetry(event.eventId());
            }
        }
    }
}

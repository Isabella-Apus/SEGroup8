package com.segroup8.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final RestClient secondhand;
    private final String internalToken;
    private final ObjectMapper json;

    OutboxPublisher(OrderRepository repository,RestClient.Builder builder,ObjectMapper json,
            @Value("${downstream.messaging-url}") String messagingUrl,
            @Value("${downstream.secondhand-url}") String secondhandUrl,
            @Value("${security.internal-service-token}") String internalToken) {
        this.repository=repository;this.messaging=builder.clone().baseUrl(messagingUrl).build();
        this.secondhand=builder.clone().baseUrl(secondhandUrl).build();
        this.internalToken=internalToken;this.json=json;
    }

    @Scheduled(fixedDelayString="${outbox.publisher.fixed-delay:2s}")
    void publish() {
        for (var event:repository.pendingOutbox()) {
            try {
                Map<String,Object> payload=json.readValue(event.payload(),Map.class);
                List<Long> recipients=repository.notificationRecipients(event.aggregateId(),event.eventType());
                if(recipients.isEmpty()) throw new IllegalStateException("order event has no notification recipient");
                payload=new LinkedHashMap<>(payload);
                payload.put("recipientUserIds",recipients);
                payload.put("displayTitle",title(event.eventType()));
                payload.put("displayText","Open the order center to view the latest update");
                payload.put("targetPath","/user/orders");
                payload.put("businessType","ORDER");
                payload.put("businessId",event.aggregateId());
                payload.put("dedupeKey",event.eventId());
                messaging.post().uri("/internal/events").header("X-Internal-Service-Token",internalToken)
                        .header("Idempotency-Key",event.eventId())
                        .header("X-Idempotency-Key",event.eventId())
                        .header("X-Request-Id",event.eventId())
                        .body(Map.of("eventId",event.eventId(),"eventType",event.eventType(),"eventVersion",1,
                                "producer","order-service","aggregateType",event.aggregateType(),
                                "aggregateId",event.aggregateId(),"occurredAt",event.createdAt(),
                                "traceId",event.eventId(),"payload",payload))
                        .retrieve().toBodilessEntity();
                publishSecondhandStatus(event,payload);
                repository.markPublished(event.eventId());
            } catch (Exception ex) {
                repository.markOutboxRetry(event.eventId());
            }
        }
    }

    private void publishSecondhandStatus(OrderRepository.OutboxMessage event,Map<String,Object> payload) {
        if(!"OrderStatusChanged.v1".equals(event.eventType())) return;
        String businessKey=repository.secondhandBusinessKey(event.aggregateId());
        if(businessKey==null) return;
        secondhand.post().uri("/internal/events/order-status-changed")
                .header("X-Internal-Service-Token",internalToken)
                .header("X-Request-Id",event.eventId())
                .header("X-Idempotency-Key",event.eventId())
                .body(Map.of("eventId",event.eventId(),"orderBusinessKey",businessKey,
                        "orderId",Long.parseLong(event.aggregateId()),"newStatus",String.valueOf(payload.get("status"))))
                .retrieve().toBodilessEntity();
    }

    private String title(String eventType) {
        return switch(eventType) {
            case "OrderStatusChanged.v1" -> "Order status updated";
            case "OrderRefundStatusChanged.v1" -> "Refund status updated";
            case "OrderShipmentReminded.v1" -> "Buyer reminded shipment";
            case "ReviewSubmitted.v1" -> "New product review";
            case "ReviewFollowUpSubmitted.v1" -> "New follow-up review";
            default -> "Order update";
        };
    }
}

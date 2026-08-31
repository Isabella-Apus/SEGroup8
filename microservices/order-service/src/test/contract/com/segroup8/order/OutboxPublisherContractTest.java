package com.segroup8.order;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@Tag("CONTRACT")
class OutboxPublisherContractTest {
    @Test
    void sendsTheSharedEventEnvelopeAndMarksOnlySuccessfulDeliveryPublished() {
        OrderRepository repository = mock(OrderRepository.class);
        var event = new OrderRepository.OutboxMessage("event-1", "OrderCreated.v1", "ORDER", "42",
                "{\"buyerId\":7}", Instant.parse("2026-08-31T12:00:00Z"));
        when(repository.pendingOutbox()).thenReturn(List.of(event));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("http://messaging.test/internal/events"))
                .andExpect(header("X-Internal-Service-Token", "internal-token"))
                .andExpect(header("X-Idempotency-Key", "event-1"))
                .andExpect(header("X-Request-Id", "event-1"))
                .andExpect(jsonPath("$.producer").value("order-service"))
                .andExpect(jsonPath("$.aggregateType").value("ORDER"))
                .andExpect(jsonPath("$.aggregateId").value("42"))
                .andExpect(jsonPath("$.payload.buyerId").value(7))
                .andRespond(withSuccess());

        new OutboxPublisher(repository, builder, new ObjectMapper(), "http://messaging.test", "internal-token")
                .publish();

        server.verify();
        verify(repository).markPublished("event-1");
    }
}

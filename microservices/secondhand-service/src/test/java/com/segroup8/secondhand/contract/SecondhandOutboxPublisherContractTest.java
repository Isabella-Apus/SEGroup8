package com.segroup8.secondhand.contract;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.secondhand.client.SecondhandOutboxPublisher;
import com.segroup8.secondhand.repository.OutboxRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SecondhandOutboxPublisherContractTest {
    @Test
    void convertsLocalNotificationFactsToTheSharedMessagingEnvelope() {
        OutboxRepository repository=mock(OutboxRepository.class);
        var row=new OutboxRepository.OutboxRow(1,"event-1","SECONDHAND_TRADE","88",
                "NotificationRequested.v1","{\"recipientIds\":[20,10],\"type\":\"CREATED\"}",0,
                Instant.parse("2026-09-01T00:00:00Z"));
        when(repository.pendingDeliveries()).thenReturn(List.of(row));
        RestClient.Builder builder=RestClient.builder();
        MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
        server.expect(once(),requestTo("http://messaging.test/internal/events"))
                .andExpect(jsonPath("$.producer").value("secondhand-service"))
                .andExpect(jsonPath("$.payload.recipientUserIds[0]").value(20))
                .andExpect(jsonPath("$.payload.dedupeKey").value("event-1"))
                .andRespond(withNoContent());
        new SecondhandOutboxPublisher(repository,new ObjectMapper().findAndRegisterModules(),builder,
                "http://messaging.test","token").publish();
        server.verify();
        verify(repository).markPublished(1);
    }
}

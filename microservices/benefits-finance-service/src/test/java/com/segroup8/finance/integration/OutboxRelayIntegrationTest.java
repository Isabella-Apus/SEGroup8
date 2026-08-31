package com.segroup8.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@SpringBootTest(classes=BenefitsFinanceApplication.class)
class OutboxRelayIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired javax.sql.DataSource dataSource;

    @BeforeEach
    void reset() {
        jdbc.update("delete from outbox_event");
        jdbc.update("insert into outbox_event(event_id,aggregate_type,aggregate_id,event_type,payload,status,available_at) "
                + "values('event-1','PAYMENT','pay-1','PaymentCompleted','{\"requestId\":\"pay-1\"}','PENDING',current_timestamp)");
    }

    @Test
    void failedDeliveryBacksOffAndLaterPublishesSameEventId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OutboxRelay relay = new OutboxRelay(JdbcClient.create(dataSource), builder.build(), "http://event-sink/internal/events",
                "test-internal-token", 10, 8);

        server.expect(once(), requestTo("http://event-sink/internal/events"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Service-Token", "test-internal-token"))
                .andExpect(header("X-Event-Id", "event-1"))
                .andExpect(header("X-Event-Type", "PaymentCompleted"))
                .andExpect(content().json("{\"requestId\":\"pay-1\"}"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        relay.publishPending();
        assertThat(jdbc.queryForObject("select status from outbox_event where event_id='event-1'", String.class))
                .isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("select attempts from outbox_event where event_id='event-1'", Integer.class)).isOne();
        server.verify();

        jdbc.update("update outbox_event set available_at=timestamp '2000-01-01 00:00:00' where event_id='event-1'");
        server.reset();
        server.expect(once(), requestTo("http://event-sink/internal/events"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Service-Token", "test-internal-token"))
                .andExpect(header("X-Event-Id", "event-1"))
                .andExpect(header("X-Event-Type", "PaymentCompleted"))
                .andExpect(content().json("{\"requestId\":\"pay-1\"}"))
                .andRespond(withSuccess());
        relay.publishPending();
        assertThat(jdbc.queryForObject("select status from outbox_event where event_id='event-1'", String.class))
                .isEqualTo("PUBLISHED");
        server.verify();
    }

    @Test
    void exhaustsBoundedRetriesIntoDeadLetterState() {
        jdbc.update("update outbox_event set attempts=2 where event_id='event-1'");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OutboxRelay relay = new OutboxRelay(JdbcClient.create(dataSource), builder.build(),
                "http://event-sink/internal/events", "test-internal-token", 10, 3);

        server.expect(once(), requestTo("http://event-sink/internal/events"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        relay.publishPending();

        assertThat(jdbc.queryForObject("select status from outbox_event where event_id='event-1'", String.class))
                .isEqualTo("DEAD");
        assertThat(jdbc.queryForObject("select attempts from outbox_event where event_id='event-1'", Integer.class))
                .isEqualTo(3);
        server.verify();
    }
}

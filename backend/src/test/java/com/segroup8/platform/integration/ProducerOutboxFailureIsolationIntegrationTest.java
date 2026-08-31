package com.segroup8.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.PayOrderRequest;
import com.segroup8.platform.event.EventTypes;
import com.segroup8.platform.event.ProducerOutboxRelay;
import com.segroup8.platform.event.ProducerOutboxService;
import com.segroup8.platform.service.OrderService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
        "app.messaging.event-notifications-enabled=true",
        "app.messaging.base-url=http://127.0.0.1:1",
        "app.internal-service.token=failure-isolation-test-token",
        "app.outbox.relay-delay-ms=3600000"
})
@ActiveProfiles("test")
@Sql(scripts = "/integration/uc12-pay-cancel-setup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ProducerOutboxFailureIsolationIntegrationTest {
    @Autowired private OrderService orders;
    @Autowired private ProducerOutboxRelay relay;
    @Autowired private ProducerOutboxService outbox;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void cleanOutbox() {
        jdbc.update("delete from outbox_event");
    }

    @AfterEach
    void clearUser() {
        UserContext.clear();
    }

    @Test
    void paymentAndOrderStateCommitWhileMessagingIsDownAndEventRemainsDurable() {
        UserContext.setUserId(1201L);
        PayOrderRequest request = new PayOrderRequest();
        request.setPayMode("COIN");
        request.setPayChannel("WECHAT");

        var paid = orders.payMyOrder(1209L, request);

        assertThat(paid.getPayStatus()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "select count(*) from order_info where voucher_id=1201 and pay_status=1", Integer.class))
                .isEqualTo(2);
        List<String> eventIds = jdbc.queryForList(
                "select event_id from outbox_event where event_type=? and status='PENDING'",
                String.class, EventTypes.PAYMENT_COMPLETED);
        assertThat(eventIds).isNotEmpty();

        relay.relay();

        assertThat(jdbc.queryForObject(
                "select count(*) from order_info where voucher_id=1201 and pay_status=1", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "select count(*) from outbox_event where event_type=? and status='RETRY' and retry_count=1",
                Integer.class, EventTypes.PAYMENT_COMPLETED)).isEqualTo(eventIds.size());
    }

    @Test
    void businessMutationAndOutboxInsertRollBackTogether() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                transactions.executeWithoutResult(status -> {
                    jdbc.update("update order_info set remark='atomic-marker' where id=1201");
                    outbox.publish(EventTypes.ORDER_STATUS_CHANGED, "order-monolith", "ORDER", 1201L,
                            java.util.Map.of("recipientUserIds", List.of(1203L),
                                    "displayTitle", "Atomic probe", "displayText", "Atomic probe",
                                    "dedupeKey", "atomic-probe:1201"));
                    throw new IllegalStateException("force rollback");
                }));

        assertThat(jdbc.queryForObject("select remark from order_info where id=1201", String.class)).isNull();
        assertThat(jdbc.queryForObject(
                "select count(*) from outbox_event where aggregate_id='1201'", Integer.class)).isZero();
    }
}

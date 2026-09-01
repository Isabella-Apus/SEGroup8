package com.segroup8.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.messaging.delivery.DeliveryWorker;
import com.segroup8.messaging.event.EventEnvelope;
import com.segroup8.messaging.event.EventTypes;
import com.segroup8.messaging.event.InboxWorker;
import com.segroup8.messaging.realtime.RealtimePublisher;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReliableMessagingIntegrationTest {
    private static final String TOKEN = "test-internal-service-token";
    private static final String OPERATIONS_TOKEN = "test-internal-operations-token";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired InboxWorker inboxWorker;
    @Autowired DeliveryWorker deliveryWorker;
    @MockBean RealtimePublisher realtime;

    @BeforeEach
    void clean() {
        reset(realtime);
        jdbc.update("delete from outbox_event");
        jdbc.update("delete from idempotency_record");
        jdbc.update("delete from inbox_event");
        jdbc.update("delete from notification");
        jdbc.update("delete from user_access_projection");
    }

    @Test
    void internalAuthenticationAndNotificationDedupeAreEnforced() throws Exception {
        String body = internalNotification("internal-dedupe-1", "trace-internal-1", "Historical order");
        mvc.perform(post("/internal/notifications").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/notifications").header("Authorization", "Bearer ordinary-user-jwt")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/notifications").headers(internal()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("Historical order"));
        mvc.perform(post("/internal/notifications").headers(internal()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("Historical order"));
        assertEquals(1, count("notification", "dedupe_key='internal-dedupe-1'"));
        assertEquals(1, count("idempotency_record", "dedupe_key='internal-dedupe-1'"));
        assertEquals(1, count("outbox_event", "dedupe_key='delivery:notification:internal-dedupe-1'"));

        String conflicting = internalNotification("internal-dedupe-1", "trace-internal-1", "Different content");
        mvc.perform(post("/internal/notifications").headers(internal()).contentType(MediaType.APPLICATION_JSON).content(conflicting))
                .andExpect(status().isConflict());
    }

    @Test
    void internalDeliveryStatusRequiresServiceToken() throws Exception {
        mvc.perform(get("/internal/delivery/lookup-key")).andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/delivery/lookup-key").header("X-Internal-Service-Token", "wrong-token"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/internal/notifications").headers(internal()).contentType(MediaType.APPLICATION_JSON)
                        .content(internalNotification("delivery-status-check", "trace-delivery-status", "Delivery access check")))
                .andExpect(status().isOk());

        mvc.perform(get("/internal/delivery/delivery-status-check").headers(internal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.persisted").value(true));
    }

    @Test
    void allSupportedContractsUseOneEnvelopeAndSnapshotWithoutSourceQueries() throws Exception {
        int index = 0;
        for (String type : EventTypes.ALL) {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (EventTypes.USER_ACCESS_CHANGED.equals(type)) {
                payload.put("userId", 900L); payload.put("status", "ACTIVE");
                payload.put("role", "USER"); payload.put("version", 3L);
            } else {
                payload.put("recipientUserIds", List.of(700L + index));
                payload.put("businessId", "missing-source-" + index);
                payload.put("displayTitle", "Historical snapshot " + index);
                payload.put("displayText", "Source service is unavailable but this snapshot remains displayable");
                payload.put("targetPath", "/history/" + index);
                payload.put("dedupeKey", "contract:" + index);
            }
            EventEnvelope event = event(type, "contract-event-" + index, payload);
            mvc.perform(post("/internal/events").headers(internal()).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(event)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.accepted").value(true));
            index++;
        }
        inboxWorker.runOnce();
        assertEquals(EventTypes.ALL.size(), count("inbox_event", "status='PROCESSED'"));
        assertEquals(EventTypes.ALL.size()-1,
                count("notification", "content like 'Source service is unavailable%'"));
        assertEquals("ACTIVE", jdbc.queryForObject(
                "select access_status from user_access_projection where user_id=900", String.class));
    }

    @Test
    void eventIdAndDedupeKeyPreventDuplicatesAcrossDeliveryAndReplay() throws Exception {
        EventEnvelope first = event(EventTypes.ORDER_STATUS_CHANGED, "event-replay-1", snapshot("business-dedupe-1"));
        accept(first, true);
        accept(first, false);
        EventEnvelope sameBusiness = event(EventTypes.ORDER_STATUS_CHANGED, "event-replay-2", snapshot("business-dedupe-1"));
        accept(sameBusiness, true);
        inboxWorker.runOnce();
        assertEquals(1, count("notification", "dedupe_key='business-dedupe-1'"));

        mvc.perform(post("/internal/events/replay/event-replay-1").headers(internal()))
                .andExpect(status().isUnauthorized());

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/internal/events/replay/event-replay-1").headers(operations())
                            .header("X-Trace-Id", "trace-replay-" + i).param("reason", "dedupe verification"))
                    .andExpect(status().isOk());
            inboxWorker.runOnce();
            assertEquals(1, count("notification", "dedupe_key='business-dedupe-1'"));
        }
        assertEquals(2, count("outbox_event", "delivery_kind='AUDIT' and event_type='REPLAY_AUDIT'"));
    }

    @Test
    void inboxRetriesThenSucceedsAndEventuallyTransitionsToDlq() throws Exception {
        EventEnvelope invalid = event(EventTypes.NOTIFICATION_REQUESTED, "retry-event", Map.of(
                "displayTitle", "missing recipient", "displayText", "temporary bad snapshot"));
        accept(invalid, true);
        inboxWorker.runOnce();
        assertEquals("RETRY", inboxStatus("retry-event"));

        EventEnvelope repaired = event(EventTypes.NOTIFICATION_REQUESTED, "retry-event", snapshot("retry-success"));
        jdbc.update("update inbox_event set payload=?,next_retry_at=current_timestamp where event_id='retry-event'",
                json.writeValueAsString(repaired));
        inboxWorker.runOnce();
        assertEquals("PROCESSED", inboxStatus("retry-event"));
        assertEquals(1, count("notification", "dedupe_key='retry-success'"));

        EventEnvelope dead = event(EventTypes.NOTIFICATION_REQUESTED, "dlq-event", Map.of(
                "displayTitle", "invalid", "displayText", "no recipient"));
        accept(dead, true);
        for (int i = 0; i < 5; i++) {
            jdbc.update("update inbox_event set next_retry_at=current_timestamp where event_id='dlq-event'");
            inboxWorker.runOnce();
        }
        assertEquals("DLQ", inboxStatus("dlq-event"));
    }

    @Test
    void userAccessEventUpdatesProjectionDisconnectsExistingSessionAndBlocksNewState() throws Exception {
        jdbc.update("insert into user_access_projection(user_id,access_status,role,source_version) values(55,'ACTIVE','USER',1)");
        EventEnvelope event = event(EventTypes.USER_ACCESS_CHANGED, "access-event-1", Map.of(
                "userId", 55L, "status", "BANNED", "role", "USER", "version", 2L));
        accept(event, true);
        inboxWorker.runOnce();
        assertEquals("BANNED", jdbc.queryForObject(
                "select access_status from user_access_projection where user_id=55", String.class));
        verify(realtime).disconnectUser(55L);
    }

    @Test
    void deliveryWorkerTracksOfflineSuccessFailureAndDlqWithoutDeletingNotification() throws Exception {
        mvc.perform(post("/internal/notifications").headers(internal()).contentType(MediaType.APPLICATION_JSON)
                        .content(internalNotification("delivery-key", "trace-delivery", "Delivery test")))
                .andExpect(status().isOk());

        when(realtime.sessionCount(77L)).thenReturn(0);
        deliveryWorker.runOnce();
        assertEquals("PENDING", rawDeliveryStatus("delivery-key"));
        assertEquals(0, retryCount("delivery-key"));

        when(realtime.sessionCount(77L)).thenReturn(1);
        when(realtime.tryPushToUser(anyLong(), anyString(), any())).thenReturn(true);
        jdbc.update("update outbox_event set next_attempt_at=current_timestamp where dedupe_key='delivery:notification:delivery-key'");
        deliveryWorker.runOnce();
        assertEquals("DELIVERED", rawDeliveryStatus("delivery-key"));
        mvc.perform(get("/internal/delivery/delivery-key").headers(internal()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.persisted").value(true))
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        mvc.perform(post("/internal/notifications").headers(internal()).contentType(MediaType.APPLICATION_JSON)
                        .content(internalNotification("delivery-fail", "trace-fail", "Failure remains durable")))
                .andExpect(status().isOk());
        when(realtime.sessionCount(77L)).thenReturn(1);
        when(realtime.tryPushToUser(anyLong(), anyString(), any())).thenReturn(false);
        for (int i = 0; i < 5; i++) {
            jdbc.update("update outbox_event set next_attempt_at=current_timestamp where dedupe_key='delivery:notification:delivery-fail'");
            deliveryWorker.runOnce();
        }
        assertEquals("DLQ", rawDeliveryStatus("delivery-fail"));
        assertEquals(1, count("notification", "dedupe_key='delivery-fail'"));
    }

    private void accept(EventEnvelope event, boolean accepted) throws Exception {
        mvc.perform(post("/internal/events").headers(internal()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(event)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.accepted").value(accepted));
    }
    private EventEnvelope event(String type, String id, Map<String, Object> payload) {
        return new EventEnvelope(id, type, 1, "integration-producer", "ORDER", "42",
                Instant.now(), "trace-" + id, payload);
    }
    private Map<String, Object> snapshot(String dedupe) {
        return Map.of("recipientUserIds", List.of(77L), "businessId", "42", "orderNo", "HIST-42",
                "displayTitle", "Historical order event", "displayText", "Snapshot survives source deletion",
                "targetPath", "/order/42", "dedupeKey", dedupe);
    }
    private String internalNotification(String dedupe, String trace, String title) throws Exception {
        return json.writeValueAsString(Map.of("recipientUserId", 77, "title", title,
                "content", "Persisted content", "notificationType", "COMPAT", "businessType", "ORDER",
                "businessId", "42", "targetPath", "/order/42", "scope", "buyer",
                "dedupeKey", dedupe, "traceId", trace));
    }
    private org.springframework.http.HttpHeaders internal() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Internal-Service-Token", TOKEN); headers.set("X-Service-Identity", "integration-producer");
        return headers;
    }
    private org.springframework.http.HttpHeaders operations() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Internal-Service-Token", OPERATIONS_TOKEN);
        headers.set("X-Service-Identity", "messaging-operator");
        return headers;
    }
    private int count(String table, String condition) {
        return jdbc.queryForObject("select count(*) from " + table + " where " + condition, Integer.class);
    }
    private String inboxStatus(String eventId) {
        return jdbc.queryForObject("select status from inbox_event where event_id=?", String.class, eventId);
    }
    private String rawDeliveryStatus(String dedupe) {
        return jdbc.queryForObject("select status from outbox_event where dedupe_key=?", String.class,
                "delivery:notification:" + dedupe);
    }
    private int retryCount(String dedupe) {
        return jdbc.queryForObject("select retry_count from outbox_event where dedupe_key=?", Integer.class,
                "delivery:notification:" + dedupe);
    }
}

package com.segroup8.messaging.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.messaging.common.ApiException;
import com.segroup8.messaging.notification.NotificationService;
import com.segroup8.messaging.notification.NotificationService.NotificationView;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalNotificationService {
    private final JdbcTemplate jdbc;
    private final NotificationService notifications;
    private final ObjectMapper json;
    public InternalNotificationService(JdbcTemplate jdbc, NotificationService notifications, ObjectMapper json) {
        this.jdbc = jdbc; this.notifications = notifications; this.json = json;
    }

    @Transactional
    public NotificationView create(InternalNotificationRequest request, String serviceIdentity) {
        String hash = hash(request);
        List<Record> existing = jdbc.query("select request_hash,notification_id from idempotency_record where dedupe_key=?",
                (rs, n) -> new Record(rs.getString(1), rs.getLong(2)), request.dedupeKey());
        if (!existing.isEmpty()) {
            if (!existing.get(0).hash().equals(hash)) throw new ApiException(409, "dedupeKey already belongs to a different request");
            return notifications.findByDedupeKey(request.dedupeKey());
        }
        NotificationView value = notifications.createReliable(request.recipientUserId(), request.title(),
                request.content(), request.targetPath(), request.scope(), request.notificationType(),
                request.businessType(), request.businessId(), null, request.dedupeKey(), request.traceId());
        try {
            jdbc.update("insert into idempotency_record(dedupe_key,service_identity,request_hash,notification_id," +
                            "response_body,created_at,updated_at) values(?,?,?,?,?,current_timestamp,current_timestamp)",
                    request.dedupeKey(), serviceIdentity, hash, value.id(), serialize(value));
        } catch (DuplicateKeyException race) {
            return notifications.findByDedupeKey(request.dedupeKey());
        }
        return value;
    }

    public DeliveryStatus delivery(String dedupeKey) {
        Integer persisted = jdbc.queryForObject("select count(*) from notification where dedupe_key=?", Integer.class, dedupeKey);
        List<Outbox> rows = jdbc.query("select status,retry_count,last_error,delivered_at from outbox_event where dedupe_key=?",
                (rs, n) -> new Outbox(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getTimestamp(4)),
                "delivery:notification:" + dedupeKey);
        if (persisted == null || persisted == 0) return new DeliveryStatus(false, "FAILED", 0, "Notification not found", null);
        if (rows.isEmpty()) return new DeliveryStatus(true, "PERSISTED", 0, null, null);
        Outbox row = rows.get(0);
        String status = switch (row.status()) {
            case "DELIVERED" -> "DELIVERED";
            case "DLQ" -> "DLQ";
            case "PENDING", "RETRY" -> "PENDING";
            default -> "FAILED";
        };
        return new DeliveryStatus(true, status, row.retries(), row.error(),
                row.deliveredAt() == null ? null : row.deliveredAt().toLocalDateTime());
    }

    private String hash(Object value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(value));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException | JsonProcessingException ex) {
            throw new IllegalStateException("Cannot hash idempotent request", ex);
        }
    }
    private String serialize(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException ex) { return "{}"; }
    }
    private record Record(String hash, long notificationId) { }
    private record Outbox(String status, int retries, String error, java.sql.Timestamp deliveredAt) { }
    public record DeliveryStatus(boolean persisted, String status, int retryCount,
            String lastError, java.time.LocalDateTime deliveredAt) { }
}

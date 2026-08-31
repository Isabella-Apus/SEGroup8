package com.segroup8.messaging.event;

import com.segroup8.messaging.common.AfterCommitExecutor;
import com.segroup8.messaging.common.ApiException;
import com.segroup8.messaging.notification.NotificationService;
import com.segroup8.messaging.realtime.RealtimePublisher;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventHandler {
    private final JdbcTemplate jdbc;
    private final NotificationService notifications;
    private final RealtimePublisher realtime;
    private final AfterCommitExecutor afterCommit;

    public EventHandler(JdbcTemplate jdbc, NotificationService notifications,
            RealtimePublisher realtime, AfterCommitExecutor afterCommit) {
        this.jdbc = jdbc; this.notifications = notifications;
        this.realtime = realtime; this.afterCommit = afterCommit;
    }

    public void handle(EventEnvelope event) {
        if (EventTypes.USER_ACCESS_CHANGED.equals(event.eventType())) {
            applyUserAccess(event);
            return;
        }
        List<Long> recipients = recipients(event.payload());
        if (recipients.isEmpty()) throw new IllegalArgumentException("Event snapshot requires recipientUserId(s)");
        String title = text(event.payload(), "displayTitle", "[Historical notification]");
        String content = text(event.payload(), "displayText", "[Historical details unavailable]");
        String targetPath = nullableText(event.payload().get("targetPath"));
        String baseDedupe = text(event.payload(), "dedupeKey", event.eventId());
        String businessId = text(event.payload(), "businessId", event.aggregateId());
        String businessType = text(event.payload(), "businessType", event.aggregateType());
        String notificationType = text(event.payload(), "notificationType", event.eventType());
        String scope = text(event.payload(), "scope", inferScope(targetPath));
        for (Long recipient : recipients) {
            String dedupe = recipients.size() == 1 ? baseDedupe : baseDedupe + ":" + recipient;
            notifications.createReliable(recipient, title, content, targetPath, scope,
                    notificationType, businessType, businessId, event.eventId(), dedupe, event.traceId());
        }
    }

    private void applyUserAccess(EventEnvelope event) {
        long userId = longValue(event.payload().get("userId"), "userId");
        String status = text(event.payload(), "status", null);
        long version = longValue(event.payload().getOrDefault("version", 0), "version");
        String role = nullableText(event.payload().get("role"));
        String displayName = nullableText(event.payload().get("displayName"));
        String avatarUrl = nullableText(event.payload().get("avatarUrl"));
        List<Long> existing = jdbc.query("select source_version from user_access_projection where user_id=?",
                (rs, n) -> rs.getLong(1), userId);
        if (existing.isEmpty()) {
            jdbc.update("insert into user_access_projection(user_id,access_status,role,display_name,avatar_url," +
                            "source_version,updated_at) values(?,?,?,?,?,?,current_timestamp)",
                    userId, status, role, displayName, avatarUrl, version);
        } else if (version >= existing.get(0)) {
            jdbc.update("update user_access_projection set access_status=?,role=?,display_name=?,avatar_url=?," +
                            "source_version=?,updated_at=current_timestamp where user_id=?",
                    status, role, displayName, avatarUrl, version, userId);
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        if ("BANNED".equals(normalized) || "DISABLED".equals(normalized)) {
            afterCommit.run(() -> realtime.disconnectUser(userId));
        }
    }

    private List<Long> recipients(Map<String, Object> payload) {
        List<Long> result = new ArrayList<>();
        Object many = payload.get("recipientUserIds");
        if (many instanceof Iterable<?> values) values.forEach(value -> result.add(longValue(value, "recipientUserIds")));
        if (result.isEmpty() && payload.get("recipientUserId") != null)
            result.add(longValue(payload.get("recipientUserId"), "recipientUserId"));
        return result.stream().distinct().toList();
    }

    private String text(Map<String, Object> payload, String name, String fallback) {
        String value = nullableText(payload.get(name));
        if (value == null || value.isBlank()) {
            if (fallback == null) throw new IllegalArgumentException(name + " is required");
            return fallback;
        }
        return value;
    }
    private String nullableText(Object value) { return value == null ? null : String.valueOf(value); }
    private long longValue(Object value, String field) {
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(String.valueOf(value)); }
        catch (RuntimeException ex) { throw new ApiException(400, field + " must be numeric"); }
    }
    private String inferScope(String path) { return path != null && path.startsWith("/merchant/") ? "seller" : "buyer"; }
}

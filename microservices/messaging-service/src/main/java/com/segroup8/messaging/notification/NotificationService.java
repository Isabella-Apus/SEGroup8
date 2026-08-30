package com.segroup8.messaging.notification;

import com.segroup8.messaging.common.AfterCommitExecutor;
import com.segroup8.messaging.common.ApiException;
import com.segroup8.messaging.realtime.RealtimePublisher;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final JdbcTemplate jdbc;
    private final RealtimePublisher realtime;
    private final AfterCommitExecutor afterCommit;
    public NotificationService(JdbcTemplate jdbc, RealtimePublisher realtime, AfterCommitExecutor afterCommit) {
        this.jdbc = jdbc; this.realtime = realtime; this.afterCommit = afterCommit;
    }

    public List<NotificationView> list(long userId, String scope) {
        String normalized = normalizeScope(scope);
        String sql = "select id,title,content,target_path,scope,is_read,create_time from notification where user_id=?"
                + (normalized == null ? "" : " and scope=?") + " order by is_read,create_time desc,id desc";
        Object[] args = normalized == null ? new Object[]{userId} : new Object[]{userId, normalized};
        return jdbc.query(sql, (rs, row) -> new NotificationView(rs.getLong("id"), rs.getString("title"),
                rs.getString("content"), rs.getString("target_path"), rs.getString("scope"),
                rs.getInt("is_read"), rs.getTimestamp("create_time").toLocalDateTime()), args);
    }

    @Transactional
    public void markRead(long userId, long id) {
        Integer count = jdbc.queryForObject("select count(*) from notification where id=? and user_id=?", Integer.class, id, userId);
        if (count == null || count == 0) throw new ApiException(404, "Notification not found");
        jdbc.update("update notification set is_read=1 where id=? and user_id=?", id, userId);
    }

    @Transactional
    public void markAllRead(long userId, String scope) {
        String normalized = normalizeScope(scope);
        if (normalized == null) jdbc.update("update notification set is_read=1 where user_id=? and is_read=0", userId);
        else jdbc.update("update notification set is_read=1 where user_id=? and scope=? and is_read=0", userId, normalized);
    }

    @Transactional
    public NotificationView create(long userId, String title, String content, String targetPath, String scope) {
        if (title == null || title.isBlank() || content == null || content.isBlank())
            throw new ApiException(400, "Notification title and content are required");
        String actualScope = scope == null ? inferScope(targetPath) : normalizeScope(scope);
        LocalDateTime now = LocalDateTime.now();
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("insert into notification(" +
                    "user_id,title,content,target_path,scope,is_read,create_time) values(?,?,?,?,?,0,?)",
                    new String[]{"id"});
            ps.setLong(1, userId); ps.setString(2, title.trim()); ps.setString(3, content.trim());
            ps.setString(4, targetPath); ps.setString(5, actualScope); ps.setTimestamp(6, Timestamp.valueOf(now));
            return ps;
        }, key);
        NotificationView value = new NotificationView(Objects.requireNonNull(key.getKey()).longValue(),
                title.trim(), content.trim(), targetPath, actualScope, 0, now);
        afterCommit.run(() -> realtime.pushToUser(userId, "NOTIFICATION_CREATED", value));
        return value;
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) return null;
        String value = scope.trim().toLowerCase(Locale.ROOT);
        return "buyer".equals(value) || "seller".equals(value) ? value : null;
    }
    private String inferScope(String path) { return path != null && path.startsWith("/merchant/") ? "seller" : "buyer"; }

    public record NotificationView(long id, String title, String content, String targetPath,
            String scope, int isRead, LocalDateTime createTime) {}
}

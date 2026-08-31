package com.segroup8.messaging.access;

import com.segroup8.messaging.common.ApiException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AccessPolicy {
    private final JdbcTemplate jdbc;
    public AccessPolicy(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public AccessSnapshot requireActive(long userId) {
        try {
            AccessSnapshot snapshot = jdbc.queryForObject(
                    "select user_id, access_status, role, display_name, avatar_url from user_access_projection where user_id=?",
                    (rs, row) -> new AccessSnapshot(rs.getLong("user_id"), rs.getString("access_status"),
                            rs.getString("role"), rs.getString("display_name"), rs.getString("avatar_url")), userId);
            if (snapshot == null || !isAllowed(snapshot.status())) {
                throw new ApiException(403, "User access is banned or disabled");
            }
            return snapshot;
        } catch (EmptyResultDataAccessException ex) {
            throw new ApiException(503, "User access state is unavailable; access denied");
        }
    }

    private boolean isAllowed(String status) {
        return "ACTIVE".equalsIgnoreCase(status) || "NORMAL".equalsIgnoreCase(status);
    }

    public record AccessSnapshot(long userId, String status, String role, String displayName, String avatarUrl) {}
}

package com.segroup8.messaging.access;

import com.segroup8.messaging.common.ApiException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class BlockPolicy {
    private final JdbcTemplate jdbc;
    private final GovernanceBlockPort fallback;
    public BlockPolicy(JdbcTemplate jdbc, GovernanceBlockPort fallback) { this.jdbc = jdbc; this.fallback = fallback; }

    public void requireCommunicationAllowed(long actor, long target, String bearerToken) {
        List<BlockSnapshot> states = jdbc.query(
                "select active,source_version from user_block_projection where "
                        + "(blocker_user_id=? and blocked_user_id=?) or (blocker_user_id=? and blocked_user_id=?)",
                (rs, row) -> new BlockSnapshot(rs.getBoolean("active"), rs.getLong("source_version")),
                actor, target, target, actor);
        if (states.stream().anyMatch(BlockSnapshot::active)) throw new ApiException(403, "Communication is blocked");
        if (states.size() == 2 && states.stream().allMatch(state -> state.sourceVersion() > 0)) return;
        boolean blocked = fallback.isCommunicationBlocked(actor, target, bearerToken)
                .orElseThrow(() -> new ApiException(503, "Block state is unavailable; communication denied"));
        if (blocked) throw new ApiException(403, "Communication is blocked");
    }

    private record BlockSnapshot(boolean active, long sourceVersion) { }
}

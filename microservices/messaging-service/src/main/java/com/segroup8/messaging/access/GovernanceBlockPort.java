package com.segroup8.messaging.access;

import java.util.Optional;

public interface GovernanceBlockPort {
    Optional<Boolean> isCommunicationBlocked(long actorUserId, long targetUserId, String bearerToken);
}

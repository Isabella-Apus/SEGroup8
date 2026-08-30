package com.segroup8.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.qos.logback.core.spi.FilterReply;
import com.segroup8.messaging.security.TokenQueryLogFilter;
import org.junit.jupiter.api.Test;

class TokenQueryLogFilterTest {
    @Test void deniesMessagesAndArgumentsContainingRealtimeJwtQuery() {
        TokenQueryLogFilter filter = new TokenQueryLogFilter();
        assertEquals(FilterReply.DENY, filter.decide(null, null, null,
                "GET /ws/realtime?token=sensitive", null, null));
        assertEquals(FilterReply.DENY, filter.decide(null, null, null,
                "New session {}", new Object[]{"ws://host/ws/realtime?token=sensitive"}, null));
        assertEquals(FilterReply.NEUTRAL, filter.decide(null, null, null,
                "GET /api/chat/conversations", null, null));
    }
}

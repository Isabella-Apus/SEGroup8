package com.segroup8.secondhand.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.segroup8.secondhand.domain.TradeOrderRequest;
import com.segroup8.secondhand.service.TradeOrderCoordinator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("DOMAIN_D")
@Tag("UNIT")
class TradeOrderCoordinatorViewUnitTest {
    @Test
    void processingStateUsesControlledDegradationMessage() {
        TradeOrderCoordinator coordinator = new TradeOrderCoordinator(null, null, null, null, null,
                null, null, null, 3);
        LocalDateTime now = LocalDateTime.now();
        TradeOrderRequest request = new TradeOrderRequest(1, "DIRECT_BUY", "1-v1",
                "SECONDHAND:DIRECT_BUY:1-v1", 1, 20, 10, new BigDecimal("50.00"), 3L,
                null, "RETRY", null, null, null, 1, "timeout", now, 1, now, now);

        assertThat(coordinator.toView(request).message()).contains("处理中");
        assertThat(coordinator.toView(request).requestStatus()).isEqualTo("RETRY");
    }
}

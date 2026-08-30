package com.segroup8.secondhand.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.segroup8.secondhand.service.TradeApplicationService;
import com.segroup8.secondhand.support.SecondhandIntegrationSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("DOMAIN_D")
@Tag("UC17")
class DirectBuyConcurrencyIntegrationTest extends SecondhandIntegrationSupport {
    @Autowired TradeApplicationService trades;

    @Test
    void concurrentBuyersCreateExactlyOneTradeAndOneOrderRequest() throws Exception {
        long productId = seedApprovedProduct(10, "并发测试教材", "80.00", true);
        var start = new CountDownLatch(1);
        List<String> outcomes = Collections.synchronizedList(new ArrayList<>());
        var executor = Executors.newFixedThreadPool(2);
        try {
            for (long buyerId : List.of(21L, 22L)) {
                executor.submit(() -> {
                    start.await();
                    try {
                        outcomes.add("OK:" + trades.buy(buyerId, productId, 1, null).requestStatus());
                    } catch (RuntimeException exception) {
                        outcomes.add("ERROR:" + exception.getMessage());
                    }
                    return null;
                });
            }
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(outcomes.stream().filter(value -> value.startsWith("OK:"))).hasSize(1);
        assertThat(outcomes.stream().filter(value -> value.startsWith("ERROR:"))).hasSize(1);
        assertThat(db.queryForObject("select count(*) from trade_order_request", Integer.class)).isEqualTo(1);
        assertThat(db.queryForObject("select status from secondhand_product where id=?", Integer.class, productId))
                .isEqualTo(3);
        verify(orderGateway, times(1)).createSecondhandOrder(org.mockito.ArgumentMatchers.any());
    }
}

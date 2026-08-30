package com.segroup8.secondhand.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.segroup8.secondhand.api.AuctionCreateRequest;
import com.segroup8.secondhand.service.TradeApplicationService;
import com.segroup8.secondhand.support.SecondhandIntegrationSupport;
import java.math.BigDecimal;
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
@Tag("UC19")
class AuctionSettlementIntegrationTest extends SecondhandIntegrationSupport {
    @Autowired TradeApplicationService trades;

    @Test
    void concurrentBidsHaveOneWinnerAndRepeatedSettlementDoesNotDuplicateOrder() throws Exception {
        long productId = seedApprovedProduct(10, "拍卖教材", "120.00", false);
        var auction = trades.createAuction(10,
                new AuctionCreateRequest(productId, new BigDecimal("50.00"), new BigDecimal("5.00"), 60));
        var start = new CountDownLatch(1);
        List<String> outcomes = Collections.synchronizedList(new ArrayList<>());
        var executor = Executors.newFixedThreadPool(2);
        try {
            executor.submit(() -> bid(start, outcomes, 21, "buyer-21", auction.id()));
            executor.submit(() -> bid(start, outcomes, 22, "buyer-22", auction.id()));
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(outcomes.stream().filter("OK"::equals)).hasSize(1);
        assertThat(db.queryForObject("select count(*) from auction_log", Integer.class)).isEqualTo(1);

        var finished = trades.closeAuction(10, auction.id());
        var repeated = trades.closeAuction(10, auction.id());
        assertThat(finished.status()).isEqualTo("FINISHED");
        assertThat(repeated.settledOrderId()).isEqualTo(finished.settledOrderId());
        assertThat(db.queryForObject("select count(*) from trade_order_request where trade_type='AUCTION'",
                Integer.class)).isEqualTo(1);
        verify(orderGateway, times(1)).createSecondhandOrder(org.mockito.ArgumentMatchers.any());
    }

    private Void bid(CountDownLatch start, List<String> outcomes, long userId, String name, long auctionId) {
        try {
            start.await();
            trades.placeBid(userId, name, auctionId, new BigDecimal("50.00"));
            outcomes.add("OK");
        } catch (RuntimeException | InterruptedException exception) {
            outcomes.add("ERROR");
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
        }
        return null;
    }
}

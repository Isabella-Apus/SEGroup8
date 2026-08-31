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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@Tag("DOMAIN_D")
@Tag("UC19")
class AuctionSettlementIntegrationTest extends SecondhandIntegrationSupport {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.6")
            .withDatabaseName("secondhand_auction_test")
            .withUsername("secondhand_app")
            .withPassword("secondhand-test-password");

    @DynamicPropertySource
    static void useRealMySql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

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

    @Test
    void independentAuctionsDoNotDeadlockOnLeadingBidHistory() throws Exception {
        List<Long> auctionIds = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            long productId = seedApprovedProduct(10, "并发拍卖教材-" + index, "120.00", false);
            auctionIds.add(trades.createAuction(10,
                    new AuctionCreateRequest(productId, new BigDecimal("50.00"),
                            new BigDecimal("5.00"), 60)).id());
        }

        var start = new CountDownLatch(1);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        var executor = Executors.newFixedThreadPool(10);
        try {
            for (int index = 0; index < auctionIds.size(); index++) {
                final int slot = index;
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int round = 0; round < 20; round++) {
                            long bidderId = 1000L + slot * 2L + round % 2;
                            trades.placeBid(bidderId, "bidder-" + bidderId, auctionIds.get(slot),
                                    new BigDecimal("50.00").add(new BigDecimal(round * 5L)));
                        }
                    } catch (Throwable failure) {
                        failures.add(failure);
                        if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
                    }
                });
            }
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(failures).isEmpty();
        assertThat(db.queryForObject("select count(*) from auction_log", Integer.class)).isEqualTo(200);
        assertThat(db.queryForObject("select count(*) from auction_log where status='LEADING'", Integer.class))
                .isEqualTo(10);
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

package com.segroup8.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.service.SecondhandTradeService;
import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Tag("DOMAIN_D")
@Tag("UC19")
@Sql(scripts = "/integration/uc19-auction-setup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SecondhandAuctionLifecycleIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.6")
            .withDatabaseName("segroup8_uc19")
            .withUsername("segroup8")
            .withPassword("segroup8_test")
            .withInitScript("schema.sql");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SecondhandTradeService secondhandTradeService;

    @SpyBean private OrderItemMapper orderItemMapper;

    private String sellerToken;
    private String bidderAToken;
    private String bidderBToken;
    private String outsiderToken;

    @BeforeEach
    void createTokens() {
        sellerToken = jwtUtils.createToken(1901L, "uc19_seller", "USER");
        bidderAToken = jwtUtils.createToken(1902L, "uc19_bidder_a", "USER");
        bidderBToken = jwtUtils.createToken(1903L, "uc19_bidder_b", "USER");
        outsiderToken = jwtUtils.createToken(1904L, "uc19_outsider", "USER");
    }

    @AfterEach
    void resetOrderItemMapper() {
        reset(orderItemMapper);
    }

    @Test
    void sellerCanCreateAfterHistoricalAuctionButDuplicateAndNonOwnerAreRejected() throws Exception {
        create(19110, sellerToken)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.productId").value(19110))
                .andExpect(jsonPath("$.data.status").value("ONGOING"))
                .andExpect(jsonPath("$.data.statusName").value("进行中"));

        create(19110, sellerToken).andExpect(jsonPath("$.code").value(400));
        create(19108, bidderAToken).andExpect(jsonPath("$.code").value(403));

        assertThat(number("SELECT COUNT(*) FROM product_auction WHERE product_id=19110 AND status='ONGOING'"))
                .isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM product_auction WHERE product_id=19110"))
                .isEqualTo(2);
    }

    @Test
    void legalBidsPersistLogsAndReleaseThePreviousBidderFunds() throws Exception {
        bid(19301, "100.00", bidderAToken)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.currentBidderUserId").value(1902))
                .andExpect(jsonPath("$.data.bidCount").value(1))
                .andExpect(jsonPath("$.data.productName").value("UC19 balance auction"));

        assertThat(money("SELECT personal_balance FROM balance WHERE user_id=1902"))
                .isEqualByComparingTo("400.00");
        assertThat(number("SELECT COUNT(*) FROM auction_log WHERE auction_id=19301 AND status='ACCEPTED'"))
                .isEqualTo(1);

        bid(19301, "120.00", bidderBToken)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.currentBidderUserId").value(1903))
                .andExpect(jsonPath("$.data.currentPrice").value(120.00))
                .andExpect(jsonPath("$.data.bidCount").value(2));

        assertThat(money("SELECT personal_balance FROM balance WHERE user_id=1902"))
                .isEqualByComparingTo("500.00");
        assertThat(money("SELECT personal_balance FROM balance WHERE user_id=1903"))
                .isEqualByComparingTo("380.00");
        assertThat(number("SELECT COUNT(*) FROM transaction_record WHERE change_type='AUCTION_BID_DEDUCT'"))
                .isEqualTo(2);
        assertThat(number("SELECT COUNT(*) FROM transaction_record WHERE change_type='AUCTION_OUTBID_REFUND'"))
                .isEqualTo(1);
    }

    @Test
    void nonexistentFutureClosedExpiredAndSelfBidsAreRejected() throws Exception {
        bid(99999, "100.00", bidderAToken).andExpect(jsonPath("$.code").value(404));
        bid(19306, "100.00", bidderAToken)
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("尚未开始")));
        bid(19307, "100.00", bidderAToken).andExpect(jsonPath("$.code").value(400));
        bid(19309, "100.00", bidderAToken).andExpect(jsonPath("$.code").value(400));
        bid(19308, "100.00", sellerToken).andExpect(jsonPath("$.code").value(400));

        assertThat(number("SELECT COUNT(*) FROM auction_log")).isZero();
        assertThat(money("SELECT personal_balance FROM balance WHERE user_id=1902"))
                .isEqualByComparingTo("500.00");
    }

    @Test
    void concurrentBidsLeaveExactlyOneLeaderAndOneFundHold() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<String> bidderA = () -> raceBid(ready, start, "110.00", bidderAToken);
            Callable<String> bidderB = () -> raceBid(ready, start, "120.00", bidderBToken);
            var resultA = executor.submit(bidderA);
            var resultB = executor.submit(bidderB);
            ready.await();
            start.countDown();

            List<JsonNode> responses = List.of(
                    objectMapper.readTree(resultA.get()),
                    objectMapper.readTree(resultB.get()));
            assertThat(responses.stream().filter(body -> body.path("code").asInt() == 0).count())
                    .isEqualTo(1);
            assertThat(number("SELECT COUNT(*) FROM auction_log WHERE auction_id=19302"))
                    .isEqualTo(1);

            long winnerId = number("SELECT current_bidder_user_id FROM product_auction WHERE id=19302");
            BigDecimal winningPrice = money("SELECT current_price FROM product_auction WHERE id=19302");
            if (winnerId == 1902L) {
                assertThat(winningPrice).isEqualByComparingTo("110.00");
                assertThat(money("SELECT personal_balance FROM balance WHERE user_id=1902"))
                        .isEqualByComparingTo("390.00");
                assertThat(money("SELECT personal_balance FROM balance WHERE user_id=1903"))
                        .isEqualByComparingTo("500.00");
            } else {
                assertThat(winnerId).isEqualTo(1903L);
                assertThat(winningPrice).isEqualByComparingTo("120.00");
                assertThat(money("SELECT personal_balance FROM balance WHERE user_id=1902"))
                        .isEqualByComparingTo("500.00");
                assertThat(money("SELECT personal_balance FROM balance WHERE user_id=1903"))
                        .isEqualByComparingTo("380.00");
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void onlySellerCanCloseAndNoBidAuctionFlowsWithoutAnOrder() throws Exception {
        close(19303, outsiderToken).andExpect(jsonPath("$.code").value(403));
        close(19303, sellerToken)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("FLOW"))
                .andExpect(jsonPath("$.data.statusName").value("已流拍"));

        assertThat(number("SELECT COUNT(*) FROM order_info")).isZero();
        assertThat(number("SELECT status FROM secondhand_product WHERE id=19103")).isEqualTo(2);
    }

    @Test
    void sellerCloseCreatesOnePaidPendingShipmentOrderAndItem() throws Exception {
        bid(19304, "100.00", bidderAToken).andExpect(jsonPath("$.code").value(0));
        MvcResult result = close(19304, sellerToken)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("FINISHED"))
                .andExpect(jsonPath("$.data.statusName").value("已成交"))
                .andReturn();
        long orderId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("settledOrderId").asLong();

        assertThat(orderId).isPositive();
        assertThat(number("SELECT COUNT(*) FROM order_info WHERE id=" + orderId
                + " AND buyer_user_id=1902 AND pay_status=1 AND order_status=1")).isEqualTo(1);
        assertThat(money("SELECT total_amount FROM order_info WHERE id=" + orderId))
                .isEqualByComparingTo("100.00");
        assertThat(number("SELECT COUNT(*) FROM order_item WHERE order_id=" + orderId
                + " AND product_type='SECONDHAND' AND product_id=19104 AND quantity=1")).isEqualTo(1);
        assertThat(money("SELECT price FROM order_item WHERE order_id=" + orderId))
                .isEqualByComparingTo("100.00");
        assertThat(number("SELECT status FROM secondhand_product WHERE id=19104")).isEqualTo(3);

        secondhandTradeService.settleExpiredAuctions();
        assertThat(number("SELECT COUNT(*) FROM order_info WHERE buyer_user_id=1902")).isEqualTo(1);
        detail(19104).andExpect(jsonPath("$.data.settledOrderId").value(orderId));
    }

    @Test
    void failedSettlementRollsBackAndCanRetryWithoutDuplicateOrder() throws Exception {
        bid(19305, "100.00", bidderAToken).andExpect(jsonPath("$.code").value(0));
        doThrow(new IllegalStateException("UC19 forced order item failure"))
                .when(orderItemMapper).insert(any(OrderItem.class));

        close(19305, sellerToken).andExpect(jsonPath("$.code").value(500));
        assertThat(text("SELECT status FROM product_auction WHERE id=19305")).isEqualTo("ONGOING");
        assertThat(number("SELECT COUNT(*) FROM order_info")).isZero();
        assertThat(number("SELECT status FROM secondhand_product WHERE id=19105")).isEqualTo(1);

        reset(orderItemMapper);
        close(19305, sellerToken)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("FINISHED"));
        assertThat(number("SELECT COUNT(*) FROM order_info")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM order_item")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM product_auction WHERE id=19305 AND settled_order_id IS NOT NULL"))
                .isEqualTo(1);
    }

    private String raceBid(CountDownLatch ready, CountDownLatch start, String amount, String token)
            throws Exception {
        ready.countDown();
        start.await();
        return bid(19302, amount, token).andReturn().getResponse().getContentAsString();
    }

    private org.springframework.test.web.servlet.ResultActions bid(long auctionId, String amount, String token)
            throws Exception {
        return mockMvc.perform(post("/api/secondhand/trade/auction/{auctionId}/bid", auctionId)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"bidAmount\":" + amount + "}"));
    }

    private org.springframework.test.web.servlet.ResultActions create(long productId, String token) throws Exception {
        return mockMvc.perform(post("/api/secondhand/trade/auction")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"productId\":" + productId
                        + ",\"startPrice\":100.00,\"incrementAmount\":10.00,\"durationMinutes\":30}"));
    }

    private org.springframework.test.web.servlet.ResultActions close(long auctionId, String token)
            throws Exception {
        return mockMvc.perform(post("/api/secondhand/trade/auction/{auctionId}/close", auctionId)
                .header("Authorization", "Bearer " + token));
    }

    private org.springframework.test.web.servlet.ResultActions detail(long productId) throws Exception {
        return mockMvc.perform(get("/api/secondhand/trade/auction/product/{productId}", productId)
                .header("Authorization", "Bearer " + bidderAToken));
    }

    private long number(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    private BigDecimal money(String sql) {
        return jdbcTemplate.queryForObject(sql, BigDecimal.class);
    }

    private String text(String sql) {
        return jdbcTemplate.queryForObject(sql, String.class);
    }
}

package com.segroup8.platform.integration;

import com.segroup8.platform.testsupport.DomainCTestTags;
import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Tag(DomainCTestTags.DOMAIN_C)
@Tag(DomainCTestTags.UC12)
@Sql(scripts = "/integration/uc12-pay-cancel-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderPayCancelUc12IntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.6")
            .withDatabaseName("segroup8_uc12")
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

    private String buyerToken;
    private String otherToken;

    @BeforeEach
    void createTokens() {
        buyerToken = jwtUtils.createToken(1201L, "uc12_buyer", "USER");
        otherToken = jwtUtils.createToken(1202L, "uc12_other", "USER");
    }

    @Test
    void coinPaymentSplitsDiscountAndPreservesAccountAndLedgerConservation() throws Exception {
        pay(1209, buyerToken, "uc12-pay-split", "COIN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(1))
                .andExpect(jsonPath("$.data.payStatus").value(1));

        assertThat(count("SELECT COUNT(*) FROM order_info WHERE voucher_id=1201 AND pay_status=1")).isEqualTo(2);
        assertThat(money("SELECT SUM(total_amount) FROM order_info WHERE voucher_id=1201 AND pay_status=1"))
                .isEqualByComparingTo("150.00");
        assertThat(money("SELECT SUM(voucher_discount_amount) FROM order_info WHERE voucher_id=1201 AND pay_status=1"))
                .isEqualByComparingTo("30.00");
        assertThat(money("SELECT SUM(seller_bear_amount) FROM order_info WHERE voucher_id=1201 AND pay_status=1"))
                .isEqualByComparingTo("20.00");
        assertThat(money("SELECT SUM(platform_bear_amount) FROM order_info WHERE voucher_id=1201 AND pay_status=1"))
                .isEqualByComparingTo("10.00");
        assertThat(money("SELECT SUM(payable_amount) FROM order_info WHERE voucher_id=1201 AND pay_status=1"))
                .isEqualByComparingTo("120.00");
        assertThat(count("SELECT COUNT(DISTINCT order_id) FROM order_item WHERE product_id IN (1201,1202) AND order_id >= 1209"))
                .isEqualTo(2);
        assertThat(money("SELECT personal_balance FROM balance WHERE user_id=1201")).isEqualByComparingTo("180.00");
        assertThat(money("SELECT business_balance FROM balance WHERE user_id=1203")).isEqualByComparingTo("50.00");
        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE order_id=1209 AND user_id=1201 AND amount=-120.00"))
                .isEqualTo(1);
        assertThat(number("SELECT status FROM user_voucher WHERE id=1203")).isEqualTo(2);
        assertThat(number("SELECT used_count FROM voucher WHERE id=1201")).isEqualTo(1);
    }

    @Test
    void onlyPendingPaymentOrderCanBePaid() throws Exception {
        pay(1204, buyerToken, "uc12-paid-state", "COIN")
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        pay(1205, buyerToken, "uc12-completed-state", "COIN")
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));

        assertThat(money("SELECT personal_balance FROM balance WHERE user_id=1201"))
                .isEqualByComparingTo("300.00");
        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE order_id IN (1204,1205)"))
                .isZero();
        assertThat(number("SELECT order_status FROM order_info WHERE id=1204")).isEqualTo(1);
        assertThat(number("SELECT order_status FROM order_info WHERE id=1205")).isEqualTo(4);
    }

    @Test
    void insufficientBalanceRollsBackOrderBalanceVoucherAndLedger() throws Exception {
        pay(1203, buyerToken, "uc12-insufficient", "COIN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        assertThat(number("SELECT order_status FROM order_info WHERE id=1203")).isZero();
        assertThat(number("SELECT pay_status FROM order_info WHERE id=1203")).isZero();
        assertThat(money("SELECT personal_balance FROM balance WHERE user_id=1201")).isEqualByComparingTo("300.00");
        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE order_id=1203")).isZero();
    }

    @Test
    void unpaidCancellationRestoresStockAndReleasesVoucher() throws Exception {
        cancel(1202, buyerToken, "uc12-cancel")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(9));

        assertThat(number("SELECT stock FROM product WHERE id=1201")).isEqualTo(21);
        assertThat(jdbcTemplate.queryForObject("SELECT used_order_id FROM user_voucher WHERE id=1202", Long.class)).isNull();
        assertThat(number("SELECT status FROM user_voucher WHERE id=1202")).isEqualTo(1);
    }

    @Test
    void paidCancellationNeverUsesUnpaidRestoreRules_andCompletedOrderIsRejected() throws Exception {
        cancel(1204, buyerToken, "uc12-paid-cancel")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertThat(number("SELECT stock FROM product WHERE id=1201")).isEqualTo(20);

        cancel(1205, buyerToken, "uc12-completed-cancel")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        assertThat(number("SELECT order_status FROM order_info WHERE id=1205")).isEqualTo(4);
    }

    @Test
    void nonBuyerCannotPayOrCancel_andNoStateChanges() throws Exception {
        pay(1206, otherToken, "uc12-other-pay", "THIRD_PARTY")
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        cancel(1207, otherToken, "uc12-other-cancel")
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));

        assertThat(number("SELECT order_status FROM order_info WHERE id=1206")).isZero();
        assertThat(number("SELECT order_status FROM order_info WHERE id=1207")).isZero();
        assertThat(number("SELECT stock FROM product WHERE id=1201")).isEqualTo(20);
    }

    @Test
    void duplicatePaymentAndCancellationReplayWithoutDuplicateSideEffects() throws Exception {
        MvcResult firstPay = pay(1206, buyerToken, "uc12-pay-replay", "COIN")
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0)).andReturn();
        MvcResult replayPay = pay(1206, buyerToken, "uc12-pay-replay", "COIN")
                .andExpect(status().isOk())
                .andExpect(header().string("X-Idempotency-Replay", "SUCCESS"))
                .andReturn();
        assertThat(replayPay.getResponse().getContentAsString()).isEqualTo(firstPay.getResponse().getContentAsString());
        assertThat(money("SELECT personal_balance FROM balance WHERE user_id=1201")).isEqualByComparingTo("240.00");
        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE order_id=1206")).isEqualTo(1);

        MvcResult firstCancel = cancel(1207, buyerToken, "uc12-cancel-replay")
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0)).andReturn();
        MvcResult replayCancel = cancel(1207, buyerToken, "uc12-cancel-replay")
                .andExpect(status().isOk())
                .andExpect(header().string("X-Idempotency-Replay", "SUCCESS"))
                .andReturn();
        assertThat(replayCancel.getResponse().getContentAsString()).isEqualTo(firstCancel.getResponse().getContentAsString());
        assertThat(number("SELECT stock FROM product WHERE id=1201")).isEqualTo(22);
        assertThat(count("SELECT COUNT(*) FROM idempotency_record WHERE user_id=1201 AND status=1")).isEqualTo(2);
    }

    @Test
    void concurrentPayAndCancelAllowsAtMostOneBusinessSideEffect() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<String> payment = () -> raceRequest(ready, start, true);
            Callable<String> cancellation = () -> raceRequest(ready, start, false);
            var paymentFuture = executor.submit(payment);
            var cancellationFuture = executor.submit(cancellation);
            ready.await();
            start.countDown();

            List<String> bodies = List.of(paymentFuture.get(), cancellationFuture.get());
            long successCount = bodies.stream().filter(body -> body.contains("\"code\":0")).count();
            assertThat(successCount).isEqualTo(1);

            long finalStatus = number("SELECT order_status FROM order_info WHERE id=1208");
            assertThat(finalStatus).isIn(1L, 9L);
            long ledgerCount = count("SELECT COUNT(*) FROM transaction_record WHERE order_id=1208");
            long stock = number("SELECT stock FROM product WHERE id=1201");
            if (finalStatus == 1L) {
                assertThat(ledgerCount).isEqualTo(1);
                assertThat(stock).isEqualTo(20);
                assertThat(money("SELECT personal_balance FROM balance WHERE user_id=1201"))
                        .isEqualByComparingTo("270.00");
            } else {
                assertThat(ledgerCount).isZero();
                assertThat(stock).isEqualTo(21);
                assertThat(money("SELECT personal_balance FROM balance WHERE user_id=1201"))
                        .isEqualByComparingTo("300.00");
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private String raceRequest(CountDownLatch ready, CountDownLatch start, boolean payment) throws Exception {
        ready.countDown();
        start.await();
        MvcResult result = payment
                ? pay(1208, buyerToken, "uc12-race-pay", "COIN").andReturn()
                : cancel(1208, buyerToken, "uc12-race-cancel").andReturn();
        return result.getResponse().getContentAsString();
    }

    private org.springframework.test.web.servlet.ResultActions pay(long orderId, String token, String key, String mode)
            throws Exception {
        return mockMvc.perform(post("/api/order/{orderId}/pay", orderId)
                .header("Authorization", "Bearer " + token)
                .header("X-Idempotency-Key", key)
                .contentType("application/json")
                .content("{\"payMode\":\"" + mode + "\",\"payChannel\":\"WECHAT\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions cancel(long orderId, String token, String key)
            throws Exception {
        return mockMvc.perform(post("/api/order/{orderId}/cancel", orderId)
                .header("Authorization", "Bearer " + token)
                .header("X-Idempotency-Key", key));
    }

    private long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    private long number(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    private BigDecimal money(String sql) {
        return jdbcTemplate.queryForObject(sql, BigDecimal.class);
    }
}

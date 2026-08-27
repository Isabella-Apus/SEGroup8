package com.segroup8.platform.integration;

import com.segroup8.platform.testsupport.DomainCTestTags;
import com.segroup8.platform.service.OrderService;
import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag(DomainCTestTags.DOMAIN_C)
@Tag(DomainCTestTags.UC13)
@Sql(scripts = "/integration/uc13-fulfillment-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class NewProductFulfillmentUc13IntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private OrderService orderService;

    private String buyerToken;
    private String sellerToken;
    private String otherToken;

    @BeforeEach
    void createTokens() {
        buyerToken = jwtUtils.createToken(1301L, "uc13_buyer", "USER");
        sellerToken = jwtUtils.createToken(1302L, "uc13_seller", "OFFICIAL_SELLER");
        otherToken = jwtUtils.createToken(1303L, "uc13_other", "USER");
    }

    @Test
    void sellerShipsNewProduct_createsOneInitialTrace_andBothPartiesCanQuery() throws Exception {
        mockMvc.perform(post("/api/order/1301/ship")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType("application/json")
                        .content("{\"originProvince\":\"\\u5317\\u4eac\",\"originCity\":\"\\u5317\\u4eac\",\"originDetail\":\"UC13 warehouse\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(2))
                .andExpect(jsonPath("$.data.logisticsStatus").value("IN_TRANSIT"));
        assertThat(count("SELECT COUNT(*) FROM logistics_trace WHERE order_id=1301")).isEqualTo(1);

        mockMvc.perform(get("/api/logistics/order/1301/trace").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get("/api/logistics/order/1301/trace").header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/logistics/order/1301/trace").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void nonSellerAndNonPendingShipAreRejected_andMergedOrderIsUnchanged() throws Exception {
        mockMvc.perform(post("/api/order/1301/ship").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(post("/api/order/1302/ship").header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(post("/api/order/1303/ship").header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(409));

        assertThat(number("SELECT order_status FROM order_info WHERE id=1301")).isEqualTo(1);
        assertThat(number("SELECT order_status FROM order_info WHERE id=1302")).isEqualTo(0);
        assertThat(number("SELECT order_status FROM order_info WHERE id=1303")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM logistics_trace WHERE order_id IN (1301,1302,1303)")).isZero();
    }

    @Test
    void receiveSettlesOnce_andConcurrentManualAutomaticConfirmationCannotDuplicateLedger() throws Exception {
        ship(1301);
        mockMvc.perform(post("/api/order/1301/confirm-receive").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(3));
        assertThat(money("SELECT business_balance FROM balance WHERE user_id=1302")).isEqualByComparingTo("120.00");
        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE order_id=1301 AND user_id=1302")).isEqualTo(1);

        mockMvc.perform(post("/api/order/1301/confirm-receive").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE order_id=1301 AND user_id=1302")).isEqualTo(1);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var manual = executor.submit(() -> {
                start.await();
                return mockMvc.perform(post("/api/order/1304/confirm-receive")
                        .header("Authorization", "Bearer " + buyerToken)).andReturn()
                        .getResponse().getContentAsString();
            });
            var automatic = executor.submit(() -> {
                start.await();
                orderService.autoConfirmReceiveForSystem(1304L);
                return "automatic";
            });
            start.countDown();
            manual.get();
            automatic.get();
        } finally {
            executor.shutdownNow();
        }
        assertThat(number("SELECT order_status FROM order_info WHERE id=1304")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE order_id=1304 AND user_id=1302")).isEqualTo(1);
    }

    @Test
    void repeatedShippingDoesNotCreateAnotherInitialTrace() throws Exception {
        ship(1301);
        mockMvc.perform(post("/api/order/1301/ship").header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        assertThat(count("SELECT COUNT(*) FROM logistics_trace WHERE order_id=1301")).isEqualTo(1);
    }

    private void ship(long orderId) throws Exception {
        mockMvc.perform(post("/api/order/{id}/ship", orderId)
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType("application/json")
                        .content("{\"originProvince\":\"\\u5317\\u4eac\",\"originCity\":\"\\u5317\\u4eac\",\"originDetail\":\"UC13 warehouse\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
    }

    private long count(String sql) { return jdbcTemplate.queryForObject(sql, Long.class); }
    private long number(String sql) { return jdbcTemplate.queryForObject(sql, Long.class); }
    private BigDecimal money(String sql) { return jdbcTemplate.queryForObject(sql, BigDecimal.class); }
}

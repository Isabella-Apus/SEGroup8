package com.segroup8.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.service.ChatService;
import com.segroup8.platform.service.NotificationService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Tag("DOMAIN_D")
@Tag("UC18")
@Sql(scripts = "/integration/uc18-bargain-setup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SecondhandNegotiationIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.6")
            .withDatabaseName("segroup8_uc18")
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

    @SpyBean private ChatService chatService;
    @SpyBean private NotificationService notificationService;

    private String sellerToken;
    private String buyerToken;
    private String otherToken;

    @BeforeEach
    void createTokens() {
        sellerToken = jwtUtils.createToken(1801L, "uc18_seller", "OFFICIAL_SELLER");
        buyerToken = jwtUtils.createToken(1802L, "uc18_buyer", "USER");
        otherToken = jwtUtils.createToken(1803L, "uc18_other", "OFFICIAL_SELLER");
    }

    @AfterEach
    void resetSideEffectSpies() {
        reset(chatService, notificationService);
    }

    @Test
    void applicationPersistsAndBothParticipantsCanListIt() throws Exception {
        long negotiationId = dataId(apply(18101, 1801, "75.00", buyerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
                .andReturn());

        assertThat(text("SELECT status FROM product_negotiation WHERE id=" + negotiationId))
                .isEqualTo("APPLIED");
        assertThat(number("SELECT COUNT(*) FROM chat_conversation WHERE source_type='SECONDHAND' AND source_id=18101"))
                .isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM chat_message WHERE content LIKE '[BARGAIN_APPLY]%'"))
                .isEqualTo(1);

        list(18101, 1802, sellerToken)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(negotiationId));
        list(18101, 1801, buyerToken)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].status").value("APPLIED"));
    }

    @Test
    void invalidNonNegotiableSelfAndRepeatedApplicationsAreRejected() throws Exception {
        apply(18102, 1801, "50.00", buyerToken).andExpect(jsonPath("$.code").value(400));
        apply(18103, 1802, "50.00", buyerToken).andExpect(jsonPath("$.code").value(400));
        apply(18101, 1801, "101.00", buyerToken).andExpect(jsonPath("$.code").value(400));
        apply(18101, 1801, "0.00", buyerToken).andExpect(jsonPath("$.code").value(400));
        apply(18101, 1801, "-1.00", buyerToken).andExpect(jsonPath("$.code").value(400));

        apply(18101, 1801, "75.00", buyerToken).andExpect(jsonPath("$.code").value(0));
        apply(18101, 1801, "70.00", buyerToken).andExpect(jsonPath("$.code").value(409));
        assertThat(number("SELECT COUNT(*) FROM product_negotiation WHERE product_id=18101 AND buyer_user_id=1802"))
                .isEqualTo(1);
    }

    @Test
    void unrelatedSellerCannotConfirmOrReject() throws Exception {
        confirm(18301, "70.00", true, otherToken).andExpect(jsonPath("$.code").value(403));
        reject(18301, otherToken).andExpect(jsonPath("$.code").value(403));
        assertThat(text("SELECT status FROM product_negotiation WHERE id=18301")).isEqualTo("APPLIED");
        assertThat(number("SELECT COUNT(*) FROM order_info")).isZero();
    }

    @Test
    void confirmationCreatesOnePendingPaymentOrderAtConfirmedPrice() throws Exception {
        MvcResult result = confirm(18301, "70.00", true, sellerToken)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("USED"))
                .andReturn();
        long orderId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("orderId").asLong();

        assertThat(orderId).isPositive();
        assertThat(text("SELECT status FROM product_negotiation WHERE id=18301")).isEqualTo("USED");
        assertThat(money("SELECT confirmed_price FROM product_negotiation WHERE id=18301"))
                .isEqualByComparingTo("70.00");
        assertThat(number("SELECT used_order_id FROM product_negotiation WHERE id=18301")).isEqualTo(orderId);
        assertThat(number("SELECT status FROM secondhand_product WHERE id=18104")).isEqualTo(3);
        assertThat(number("SELECT COUNT(*) FROM order_info WHERE id=" + orderId + " AND order_status=0 AND pay_status=0"))
                .isEqualTo(1);
        assertThat(money("SELECT total_amount FROM order_info WHERE id=" + orderId)).isEqualByComparingTo("70.00");
        assertThat(money("SELECT price FROM order_item WHERE order_id=" + orderId)).isEqualByComparingTo("70.00");
    }

    @Test
    void rejectionEndsApplicationWithoutCreatingOrder() throws Exception {
        reject(18302, sellerToken)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        assertThat(text("SELECT status FROM product_negotiation WHERE id=18302")).isEqualTo("REJECTED");
        assertThat(number("SELECT status FROM secondhand_product WHERE id=18105")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM order_info")).isZero();
    }

    @Test
    void concurrentConfirmAndRejectProduceExactlyOneDecision() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<String> confirm = () -> raceDecision(ready, start, true);
            Callable<String> reject = () -> raceDecision(ready, start, false);
            var confirmResult = executor.submit(confirm);
            var rejectResult = executor.submit(reject);
            ready.await();
            start.countDown();

            List<String> responses = List.of(confirmResult.get(), rejectResult.get());
            assertThat(responses.stream().filter(body -> body.contains("\"code\":0")).count()).isEqualTo(1);
            String finalStatus = text("SELECT status FROM product_negotiation WHERE id=18303");
            assertThat(finalStatus).isIn("USED", "REJECTED");
            if ("USED".equals(finalStatus)) {
                assertThat(number("SELECT COUNT(*) FROM order_item WHERE product_id=18106")).isEqualTo(1);
                assertThat(number("SELECT status FROM secondhand_product WHERE id=18106")).isEqualTo(3);
            } else {
                assertThat(number("SELECT COUNT(*) FROM order_info")).isZero();
                assertThat(number("SELECT status FROM secondhand_product WHERE id=18106")).isEqualTo(1);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void chatAndNotificationFailuresDoNotRollbackCoreDecision() throws Exception {
        doThrow(new IllegalStateException("UC18 forced chat failure"))
                .when(chatService).createOrGetConversation(anyLong(), anyLong(), anyString(), anyLong());
        long negotiationId = dataId(apply(18107, 1801, "55.00", buyerToken)
                .andExpect(jsonPath("$.code").value(0))
                .andReturn());
        assertThat(text("SELECT status FROM product_negotiation WHERE id=" + negotiationId)).isEqualTo("APPLIED");

        reset(chatService);
        doThrow(new IllegalStateException("UC18 forced notification failure"))
                .when(notificationService).createNotification(anyLong(), anyString(), anyString(), anyString());
        confirm(negotiationId, "55.00", false, sellerToken)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
        assertThat(text("SELECT status FROM product_negotiation WHERE id=" + negotiationId))
                .isEqualTo("CONFIRMED");
        assertThat(number("SELECT COUNT(*) FROM order_info")).isZero();
    }

    private String raceDecision(CountDownLatch ready, CountDownLatch start, boolean shouldConfirm)
            throws Exception {
        ready.countDown();
        start.await();
        return (shouldConfirm
                ? confirm(18303, "60.00", true, sellerToken)
                : reject(18303, sellerToken))
                .andReturn().getResponse().getContentAsString();
    }

    private org.springframework.test.web.servlet.ResultActions apply(long productId, long sellerId,
            String price, String token) throws Exception {
        return mockMvc.perform(post("/api/secondhand/trade/bargain/apply")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"productId\":" + productId + ",\"sellerUserId\":" + sellerId
                        + ",\"proposedPrice\":" + price + "}"));
    }

    private org.springframework.test.web.servlet.ResultActions confirm(long negotiationId, String price,
            boolean createOrder, String token) throws Exception {
        return mockMvc.perform(post("/api/secondhand/trade/bargain/confirm")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"negotiationId\":" + negotiationId + ",\"confirmedPrice\":" + price
                        + ",\"createOrder\":" + createOrder + "}"));
    }

    private org.springframework.test.web.servlet.ResultActions reject(long negotiationId, String token)
            throws Exception {
        return mockMvc.perform(post("/api/secondhand/trade/bargain/{negotiationId}/reject", negotiationId)
                .header("Authorization", "Bearer " + token));
    }

    private org.springframework.test.web.servlet.ResultActions list(long productId, long counterpartId,
            String token) throws Exception {
        return mockMvc.perform(get("/api/secondhand/trade/bargain/list")
                .header("Authorization", "Bearer " + token)
                .param("productId", String.valueOf(productId))
                .param("counterpartUserId", String.valueOf(counterpartId)));
    }

    private long dataId(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("id").asLong();
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

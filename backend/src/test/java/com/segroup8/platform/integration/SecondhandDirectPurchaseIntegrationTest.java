package com.segroup8.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.mapper.OrderInfoMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Tag("DOMAIN_D")
@Tag("UC17")
@Sql(scripts = "/integration/uc17-direct-purchase-setup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SecondhandDirectPurchaseIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.6")
            .withDatabaseName("segroup8_uc17")
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

    @SpyBean
    private OrderInfoMapper orderInfoMapper;

    private String sellerToken;
    private String buyerAToken;
    private String buyerBToken;

    @BeforeEach
    void createTokens() {
        sellerToken = jwtUtils.createToken(1701L, "uc17_seller", "USER");
        buyerAToken = jwtUtils.createToken(1702L, "uc17_buyer_a", "USER");
        buyerBToken = jwtUtils.createToken(1703L, "uc17_buyer_b", "USER");
    }

    @AfterEach
    void resetMapperSpy() {
        reset(orderInfoMapper);
    }

    @Test
    void availableProductAndOwnedAddressCreateOnePendingOrderAtomically() throws Exception {
        MvcResult result = buy(17101, buyerAToken, 17201)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(0))
                .andExpect(jsonPath("$.data.payStatus").value(0))
                .andExpect(jsonPath("$.data.totalAmount").value(100.0))
                .andReturn();

        long orderId = dataId(result);
        assertThat(number("SELECT status FROM secondhand_product WHERE id=17101")).isEqualTo(3);
        assertThat(number("SELECT COUNT(*) FROM order_info WHERE id=" + orderId + " AND buyer_user_id=1702 AND order_status=0 AND pay_status=0"))
                .isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM order_item WHERE order_id=" + orderId + " AND product_type='SECONDHAND' AND product_id=17101"))
                .isEqualTo(1);
        assertThat(text("SELECT receiver_detail_address FROM order_info WHERE id=" + orderId))
                .isEqualTo("UC17 Road A");
    }

    @Test
    void offShelfSoldAndSelfOwnedProductsAreRejectedWithoutOrders() throws Exception {
        buy(17102, buyerAToken, 17201).andExpect(jsonPath("$.code").value(400));
        buy(17103, buyerAToken, 17201).andExpect(jsonPath("$.code").value(400));
        buy(17104, buyerAToken, 17201).andExpect(jsonPath("$.code").value(400));

        assertThat(number("SELECT COUNT(*) FROM order_info")).isZero();
        assertThat(number("SELECT status FROM secondhand_product WHERE id=17102")).isEqualTo(2);
        assertThat(number("SELECT status FROM secondhand_product WHERE id=17103")).isEqualTo(3);
        assertThat(number("SELECT status FROM secondhand_product WHERE id=17104")).isEqualTo(1);
    }

    @Test
    void missingAndForeignAddressesAreRejectedBeforeProductReservation() throws Exception {
        mockMvc.perform(post("/api/secondhand/17101/buy")
                        .header("Authorization", "Bearer " + buyerAToken)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        buy(17101, buyerAToken, 17203).andExpect(jsonPath("$.code").value(400));
        buy(17101, buyerAToken, 999999).andExpect(jsonPath("$.code").value(400));

        assertThat(number("SELECT status FROM secondhand_product WHERE id=17101")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM order_info")).isZero();
    }

    @Test
    void twoConcurrentBuyersProduceExactlyOneOrderAndOneWinner() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<String> buyerA = () -> raceBuy(ready, start, buyerAToken, 17201);
            Callable<String> buyerB = () -> raceBuy(ready, start, buyerBToken, 17202);
            var a = executor.submit(buyerA);
            var b = executor.submit(buyerB);
            ready.await();
            start.countDown();

            List<String> responses = List.of(a.get(), b.get());
            assertThat(responses.stream().filter(body -> body.contains("\"code\":0")).count()).isEqualTo(1);
            assertThat(number("SELECT COUNT(*) FROM order_info")).isEqualTo(1);
            assertThat(number("SELECT COUNT(*) FROM order_item WHERE product_id=17105")).isEqualTo(1);
            assertThat(number("SELECT status FROM secondhand_product WHERE id=17105")).isEqualTo(3);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void orderInsertFailureRollsBackReservedProduct() throws Exception {
        doThrow(new IllegalStateException("UC17 forced order insert failure"))
                .when(orderInfoMapper).insert(any(OrderInfo.class));

        buy(17106, buyerAToken, 17201)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        assertThat(number("SELECT status FROM secondhand_product WHERE id=17106")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM order_info")).isZero();
        assertThat(number("SELECT COUNT(*) FROM order_item")).isZero();
    }

    @Test
    void unpaidCancellationRelistsProductWhilePaymentMovesOrderToPendingShipment() throws Exception {
        long cancelOrderId = dataId(buy(17107, buyerAToken, 17201)
                .andExpect(jsonPath("$.code").value(0)).andReturn());
        mockMvc.perform(post("/api/order/{orderId}/cancel", cancelOrderId)
                        .header("Authorization", "Bearer " + buyerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(9));
        assertThat(number("SELECT status FROM secondhand_product WHERE id=17107")).isEqualTo(1);

        long payOrderId = dataId(buy(17108, buyerAToken, 17201)
                .andExpect(jsonPath("$.code").value(0)).andReturn());
        mockMvc.perform(post("/api/order/{orderId}/pay", payOrderId)
                        .header("Authorization", "Bearer " + buyerAToken)
                        .contentType("application/json")
                        .content("{\"payMode\":\"THIRD_PARTY\",\"payChannel\":\"WECHAT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(1))
                .andExpect(jsonPath("$.data.payStatus").value(1));
        assertThat(number("SELECT status FROM secondhand_product WHERE id=17108")).isEqualTo(3);
    }

    @Test
    void duplicateClickCreatesOneDealAndNegotiatedPriceHonorsEffectiveWindow() throws Exception {
        buy(17111, buyerAToken, 17201).andExpect(jsonPath("$.code").value(0));
        buy(17111, buyerAToken, 17201).andExpect(jsonPath("$.code").value(400));
        assertThat(number("SELECT COUNT(*) FROM order_item WHERE product_id=17111")).isEqualTo(1);

        long negotiatedOrderId = dataId(buy(17109, buyerAToken, 17201)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalAmount").value(60.0))
                .andReturn());
        assertThat(money("SELECT price FROM order_item WHERE order_id=" + negotiatedOrderId))
                .isEqualByComparingTo("60.00");
        assertThat(text("SELECT status FROM product_negotiation WHERE id=17301")).isEqualTo("USED");

        buy(17110, buyerAToken, 17201).andExpect(jsonPath("$.code").value(400));
        assertThat(number("SELECT status FROM secondhand_product WHERE id=17110")).isEqualTo(1);

        long futureOrderId = dataId(buy(17112, buyerAToken, 17201)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalAmount").value(100.0))
                .andReturn());
        assertThat(money("SELECT price FROM order_item WHERE order_id=" + futureOrderId))
                .isEqualByComparingTo("100.00");
    }

    private String raceBuy(CountDownLatch ready, CountDownLatch start, String token, long addressId)
            throws Exception {
        ready.countDown();
        start.await();
        return buy(17105, token, addressId).andReturn().getResponse().getContentAsString();
    }

    private org.springframework.test.web.servlet.ResultActions buy(long productId, String token, long addressId)
            throws Exception {
        return mockMvc.perform(post("/api/secondhand/{productId}/buy", productId)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"addressId\":" + addressId + "}"));
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

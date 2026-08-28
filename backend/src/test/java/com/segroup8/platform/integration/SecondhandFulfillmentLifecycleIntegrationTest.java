package com.segroup8.platform.integration;

import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.service.NotificationService;
import com.segroup8.platform.service.settlement.EscrowSettlementService;
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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
@Tag("UC20")
@Sql(scripts = "/integration/uc20-fulfillment-setup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SecondhandFulfillmentLifecycleIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.6")
            .withDatabaseName("segroup8_uc20")
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

    @SpyBean private NotificationService notificationService;
    @SpyBean private EscrowSettlementService escrowSettlementService;
    @SpyBean private OrderInfoMapper orderInfoMapper;

    private String sellerToken;
    private String buyerToken;
    private String outsiderToken;

    @BeforeEach
    void createTokens() {
        sellerToken = jwtUtils.createToken(2001L, "uc20_seller", "USER");
        buyerToken = jwtUtils.createToken(2002L, "uc20_buyer", "USER");
        outsiderToken = jwtUtils.createToken(2003L, "uc20_outsider", "USER");
    }

    @AfterEach
    void resetSpies() {
        reset(notificationService, escrowSettlementService, orderInfoMapper);
    }

    @Test
    void shipmentRequiresSellerOwnershipPaymentAndPendingShipmentState() throws Exception {
        ship(20301, outsiderToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
        ship(20302, sellerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("未支付")));
        ship(20303, sellerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        confirmReceive(20302, buyerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        assertThat(number("SELECT order_status FROM order_info WHERE id=20301")).isEqualTo(1);
        assertThat(number("SELECT order_status FROM order_info WHERE id=20302")).isEqualTo(1);
        assertThat(number("SELECT order_status FROM order_info WHERE id=20303")).isZero();
        assertThat(number("SELECT COUNT(*) FROM logistics_trace WHERE order_id IN (20301,20302,20303)"))
                .isZero();
    }

    @Test
    void repeatedShipmentIsIdempotentAndCreatesOneInitialTrace() throws Exception {
        ship(20301, sellerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(2))
                .andExpect(jsonPath("$.data.logisticsStatus").value("IN_TRANSIT"));
        LocalDateTime firstShippedTime = timestamp("SELECT shipped_time FROM order_info WHERE id=20301");

        ship(20301, sellerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(2));

        assertThat(timestamp("SELECT shipped_time FROM order_info WHERE id=20301"))
                .isEqualTo(firstShippedTime);
        assertThat(number("SELECT version FROM order_info WHERE id=20301")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM logistics_trace WHERE order_id=20301")).isEqualTo(1);
        trace(20301, buyerToken)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].statusDesc").value("包裹已揽收"));
    }

    @Test
    void onlyBuyerCanConfirmAndRepeatedReceiptSettlesExactlyOnce() throws Exception {
        confirmReceive(20304, outsiderToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
        trace(20304, buyerToken)
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1));

        confirmReceive(20304, buyerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(3))
                .andExpect(jsonPath("$.data.orderStatusName").value("待评价"));

        assertThat(money("SELECT personal_balance FROM balance WHERE user_id=2001"))
                .isEqualByComparingTo("100.00");
        assertThat(number("SELECT COUNT(*) FROM transaction_record WHERE order_id=20304 AND change_type='ESCROW_RELEASE_PERSONAL'"))
                .isEqualTo(1);
        assertThat(number("SELECT can_refund FROM order_info WHERE id=20304")).isZero();
        assertThat(timestamp("SELECT received_time FROM order_info WHERE id=20304")).isNotNull();

        confirmReceive(20304, buyerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        assertThat(money("SELECT personal_balance FROM balance WHERE user_id=2001"))
                .isEqualByComparingTo("100.00");
        assertThat(number("SELECT COUNT(*) FROM transaction_record WHERE order_id=20304 AND change_type='ESCROW_RELEASE_PERSONAL'"))
                .isEqualTo(1);
    }

    @Test
    void notificationFailuresDoNotRollbackShipmentOrReceipt() throws Exception {
        doThrow(new IllegalStateException("UC20 forced shipment notification failure"))
                .when(notificationService).createNotification(anyLong(), anyString(), anyString(), anyString());
        ship(20305, sellerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(2));
        assertThat(number("SELECT order_status FROM order_info WHERE id=20305")).isEqualTo(2);
        assertThat(number("SELECT COUNT(*) FROM logistics_trace WHERE order_id=20305")).isEqualTo(1);

        reset(notificationService);
        doThrow(new IllegalStateException("UC20 forced receipt notification failure"))
                .when(notificationService).createNotification(anyLong(), anyString(), anyString(), anyString());
        confirmReceive(20306, buyerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(3));
        assertThat(number("SELECT order_status FROM order_info WHERE id=20306")).isEqualTo(3);
        assertThat(money("SELECT personal_balance FROM balance WHERE user_id=2001"))
                .isEqualByComparingTo("70.00");
        assertThat(number("SELECT COUNT(*) FROM transaction_record WHERE order_id=20306 AND change_type='ESCROW_RELEASE_PERSONAL'"))
                .isEqualTo(1);
    }

    @Test
    void settlementFailureRollsBackReceiptAndRetryDoesNotDuplicateCredit() throws Exception {
        doThrow(new IllegalStateException("UC20 forced settlement failure"))
                .when(escrowSettlementService).releaseEscrow(any(OrderInfo.class), anyList());
        confirmReceive(20307, buyerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        assertThat(number("SELECT order_status FROM order_info WHERE id=20307")).isEqualTo(2);
        assertThat(money("SELECT personal_balance FROM balance WHERE user_id=2001"))
                .isEqualByComparingTo("10.00");
        assertThat(number("SELECT COUNT(*) FROM transaction_record WHERE order_id=20307")).isZero();

        reset(escrowSettlementService);
        confirmReceive(20307, buyerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(3));
        assertThat(money("SELECT personal_balance FROM balance WHERE user_id=2001"))
                .isEqualByComparingTo("65.00");
        assertThat(number("SELECT COUNT(*) FROM transaction_record WHERE order_id=20307 AND change_type='ESCROW_RELEASE_PERSONAL'"))
                .isEqualTo(1);
    }

    @Test
    void bargainOrderCreationFailureRestoresNegotiationAndProduct() throws Exception {
        doThrow(new IllegalStateException("UC20 forced bargain order creation failure"))
                .when(orderInfoMapper).insert(any(OrderInfo.class));

        mockMvc.perform(post("/api/secondhand/trade/bargain/confirm")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType("application/json")
                        .content("""
                                {"negotiationId":20501,"confirmedPrice":45.00,"createOrder":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        assertThat(text("SELECT status FROM product_negotiation WHERE id=20501")).isEqualTo("APPLIED");
        assertThat(number("SELECT COUNT(*) FROM order_info")).isEqualTo(7);
        assertThat(number("SELECT COUNT(*) FROM order_item")).isEqualTo(7);
        assertThat(number("SELECT status FROM secondhand_product WHERE id=20208")).isEqualTo(1);
    }

    private org.springframework.test.web.servlet.ResultActions ship(long orderId, String token) throws Exception {
        return mockMvc.perform(post("/api/order/{orderId}/ship", orderId)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                        {"originProvince":"广东省","originCity":"广州市","originDetail":"天河区1号"}
                        """));
    }

    private org.springframework.test.web.servlet.ResultActions confirmReceive(long orderId, String token)
            throws Exception {
        return mockMvc.perform(post("/api/order/{orderId}/confirm-receive", orderId)
                .header("Authorization", "Bearer " + token));
    }

    private org.springframework.test.web.servlet.ResultActions trace(long orderId, String token) throws Exception {
        return mockMvc.perform(get("/api/logistics/order/{orderId}/trace", orderId)
                .header("Authorization", "Bearer " + token));
    }

    private long number(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    private BigDecimal money(String sql) {
        return jdbcTemplate.queryForObject(sql, BigDecimal.class);
    }

    private LocalDateTime timestamp(String sql) {
        return jdbcTemplate.queryForObject(sql, LocalDateTime.class);
    }

    private String text(String sql) {
        return jdbcTemplate.queryForObject(sql, String.class);
    }
}

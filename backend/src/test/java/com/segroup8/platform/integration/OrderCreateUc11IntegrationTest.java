package com.segroup8.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag(DomainCTestTags.DOMAIN_C)
@Tag(DomainCTestTags.UC11)
@Sql(scripts = "/integration/uc11-order-create-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderCreateUc11IntegrationTest {

    private static final long BUYER_ID = 1101L;
    private static final long SELLER_ID = 1102L;
    private static final long ADDRESS_ID = 1101L;
    private static final long PRODUCT_ID = 1101L;
    private static final long VALID_VOUCHER_ID = 1110L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String buyerToken;

    @BeforeEach
    void createBuyerToken() {
        buyerToken = jwtUtils.createToken(BUYER_ID, "uc11_buyer", "USER");
    }

    @Test
    void createsOrderFromServerPrice_mergesItems_andPersistsResponseConsistently() throws Exception {
        String request = """
                {
                  "addressId": 1101,
                  "remark": "UC11 checkout",
                  "items": [
                    {"productId": 1101, "quantity": 2},
                    {"productId": 1101, "quantity": 1}
                  ]
                }
                """;

        MvcResult result = createOrder(request, "uc11-success")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(0))
                .andExpect(jsonPath("$.data.payStatus").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3))
                .andExpect(jsonPath("$.data.items[0].price").value(19.9))
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        long orderId = payload.at("/data/id").asLong();
        assertThat(payload.at("/data/totalAmount").decimalValue()).isEqualByComparingTo("59.70");
        assertThat(payload.at("/data/payableAmount").decimalValue()).isEqualByComparingTo("59.70");

        assertThat(count("SELECT COUNT(*) FROM order_info WHERE id = ? AND buyer_user_id = ?", orderId, BUYER_ID))
                .isEqualTo(1);
        assertThat(money("SELECT total_amount FROM order_info WHERE id = ?", orderId))
                .isEqualByComparingTo("59.70");
        assertThat(money("SELECT payable_amount FROM order_info WHERE id = ?", orderId))
                .isEqualByComparingTo("59.70");
        assertThat(count("SELECT COUNT(*) FROM order_item WHERE order_id = ?", orderId)).isEqualTo(1);
        assertThat(number("SELECT quantity FROM order_item WHERE order_id = ?", orderId)).isEqualTo(3);
        assertThat(money("SELECT price FROM order_item WHERE order_id = ?", orderId))
                .isEqualByComparingTo("19.90");
        assertThat(number("SELECT stock FROM product WHERE id = ?", PRODUCT_ID)).isEqualTo(7);

        mockMvc.perform(get("/api/order/detail/{orderId}", orderId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.receiverName").value("UC11 Buyer"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3));
    }

    @Test
    void rejectsInvalidProductAddressSelfPurchaseAndBlockRelationships_withoutSideEffects() throws Exception {
        assertRejected(orderRequest(1103, 1, ADDRESS_ID, null), "uc11-off-shelf", 400);
        assertRejected(orderRequest(1104, 2, ADDRESS_ID, null), "uc11-stock", 400);
        assertRejected(orderRequest(PRODUCT_ID, 1, 1102L, null), "uc11-address-owner", 400);
        assertRejected(orderRequest(1105, 1, ADDRESS_ID, null), "uc11-self-purchase", 403);

        jdbcTemplate.update("INSERT INTO user_block (blocker_id, blocked_id) VALUES (?, ?)", BUYER_ID, SELLER_ID);
        assertRejected(orderRequest(PRODUCT_ID, 1, ADDRESS_ID, null), "uc11-buyer-blocks", 403);
        jdbcTemplate.update("DELETE FROM user_block WHERE blocker_id = ? AND blocked_id = ?", BUYER_ID, SELLER_ID);
        jdbcTemplate.update("INSERT INTO user_block (blocker_id, blocked_id) VALUES (?, ?)", SELLER_ID, BUYER_ID);
        assertRejected(orderRequest(PRODUCT_ID, 1, ADDRESS_ID, null), "uc11-seller-blocks", 403);

        assertNoCreatedOrder();
        assertThat(number("SELECT stock FROM product WHERE id = ?", PRODUCT_ID)).isEqualTo(10);
    }

    @Test
    void rejectsInvalidRequestParameters_beforeWritingBusinessData() throws Exception {
        assertValidationRejected("{\"addressId\":1101,\"items\":[]}", "uc11-empty");
        assertValidationRejected(orderRequest(PRODUCT_ID, 0, ADDRESS_ID, null), "uc11-zero-quantity");

        assertNoCreatedOrder();
        assertThat(number("SELECT stock FROM product WHERE id = ?", PRODUCT_ID)).isEqualTo(10);
    }

    @Test
    void appliesEligibleVoucher_andKeepsOrderInventoryAndVoucherConsistent() throws Exception {
        MvcResult result = createOrder(orderRequest(PRODUCT_ID, 3, ADDRESS_ID, VALID_VOUCHER_ID), "uc11-voucher")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.voucherId").value(VALID_VOUCHER_ID))
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        long orderId = payload.at("/data/id").asLong();
        assertThat(payload.at("/data/totalAmount").decimalValue()).isEqualByComparingTo("59.70");
        assertThat(payload.at("/data/voucherDiscountAmount").decimalValue()).isEqualByComparingTo("10.00");
        assertThat(payload.at("/data/sellerBearAmount").decimalValue()).isEqualByComparingTo("10.00");
        assertThat(payload.at("/data/platformBearAmount").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(payload.at("/data/payableAmount").decimalValue()).isEqualByComparingTo("49.70");

        assertThat(money("SELECT payable_amount FROM order_info WHERE id = ?", orderId))
                .isEqualByComparingTo("49.70");
        assertThat(number("SELECT stock FROM product WHERE id = ?", PRODUCT_ID)).isEqualTo(7);
        assertThat(number("SELECT status FROM user_voucher WHERE id = 1110")).isEqualTo(1);
        assertThat(number("SELECT used_order_id FROM user_voucher WHERE id = 1110")).isEqualTo(orderId);
    }

    @Test
    void rejectsUnclaimedThresholdAndShopMismatchVouchers_andRollsBackEverything() throws Exception {
        long[] voucherIds = {1111L, 1112L, 1113L, 999999L};
        for (long voucherId : voucherIds) {
            assertRejected(
                    orderRequest(PRODUCT_ID, 3, ADDRESS_ID, voucherId),
                    "uc11-invalid-voucher-" + voucherId,
                    voucherId == 999999L ? 404 : 400);
            assertNoCreatedOrder();
            assertThat(number("SELECT stock FROM product WHERE id = ?", PRODUCT_ID)).isEqualTo(10);
        }

        assertThat(count("SELECT COUNT(*) FROM user_voucher WHERE used_order_id IS NOT NULL AND user_id = ?", BUYER_ID))
                .isZero();
    }

    @Test
    void laterItemPersistenceFailure_rollsBackOrderInventoryAndOccupiedVoucher() throws Exception {
        jdbcTemplate.update("UPDATE product SET name = ? WHERE id = 1102", "X".repeat(110));
        String request = """
                {
                  "addressId": 1101,
                  "voucherId": 1110,
                  "items": [
                    {"productId": 1101, "quantity": 2},
                    {"productId": 1102, "quantity": 2}
                  ]
                }
                """;

        assertRejected(request, "uc11-transaction-rollback", 400);

        assertNoCreatedOrder();
        assertThat(number("SELECT stock FROM product WHERE id = 1101")).isEqualTo(10);
        assertThat(number("SELECT stock FROM product WHERE id = 1102")).isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT used_order_id FROM user_voucher WHERE id = 1110", Long.class)).isNull();
    }

    @Test
    void duplicateIdempotencyKey_replaysResponseAndCreatesOnlyOneOrder() throws Exception {
        String request = orderRequest(1102, 2, ADDRESS_ID, null);

        MvcResult first = createOrder(request, "uc11-idempotent")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        MvcResult replay = createOrder(request, "uc11-idempotent")
                .andExpect(status().isOk())
                .andExpect(header().string("X-Idempotency-Replay", "SUCCESS"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        assertThat(replay.getResponse().getContentAsString())
                .isEqualTo(first.getResponse().getContentAsString());
        assertThat(count("SELECT COUNT(*) FROM order_info WHERE buyer_user_id = ?", BUYER_ID)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM order_item WHERE order_id IN "
                + "(SELECT id FROM order_info WHERE buyer_user_id = ?)", BUYER_ID)).isEqualTo(1);
        assertThat(number("SELECT stock FROM product WHERE id = 1102")).isEqualTo(6);
        assertThat(count("SELECT COUNT(*) FROM idempotency_record WHERE user_id = ? "
                + "AND idempotency_key = 'uc11-idempotent' AND status = 1", BUYER_ID)).isEqualTo(1);
    }

    private org.springframework.test.web.servlet.ResultActions createOrder(String request, String idempotencyKey)
            throws Exception {
        return mockMvc.perform(post("/api/order/create")
                .header("Authorization", "Bearer " + buyerToken)
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .content(request));
    }

    private void assertRejected(String request, String idempotencyKey, int expectedCode) throws Exception {
        createOrder(request, idempotencyKey)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private void assertValidationRejected(String request, String idempotencyKey) throws Exception {
        createOrder(request, idempotencyKey)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private void assertNoCreatedOrder() {
        assertThat(count("SELECT COUNT(*) FROM order_info WHERE buyer_user_id = ?", BUYER_ID)).isZero();
        assertThat(count("SELECT COUNT(*) FROM order_item WHERE order_id IN "
                + "(SELECT id FROM order_info WHERE buyer_user_id = ?)", BUYER_ID)).isZero();
    }

    private String orderRequest(long productId, int quantity, long addressId, Long voucherId) {
        String voucher = voucherId == null ? "null" : voucherId.toString();
        return "{\"addressId\":" + addressId
                + ",\"voucherId\":" + voucher
                + ",\"items\":[{\"productId\":" + productId
                + ",\"quantity\":" + quantity + "}]}";
    }

    private long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private long number(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private BigDecimal money(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
    }
}

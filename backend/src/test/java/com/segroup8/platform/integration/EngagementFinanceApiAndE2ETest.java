package com.segroup8.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.common.TransactionTradeTypeEnum;
import com.segroup8.platform.realtime.RealtimeHandshakeInterceptor;
import com.segroup8.platform.realtime.RealtimePushService;
import com.segroup8.platform.realtime.RealtimeWebSocketHandler;
import com.segroup8.platform.service.NotificationService;
import com.segroup8.platform.service.settlement.EscrowSettlementService;
import com.segroup8.platform.utils.JwtUtils;
import com.segroup8.platform.vo.NotificationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EngagementFinanceApiAndE2ETest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate db;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private EscrowSettlementService escrowSettlementService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RealtimePushService realtimePushService;

    @Autowired
    private RealtimeWebSocketHandler realtimeWebSocketHandler;

    @Autowired
    private RealtimeHandshakeInterceptor realtimeHandshakeInterceptor;

    private String buyerToken;
    private String adminToken;
    private String sellerToken;

    @BeforeEach
    void resetEvidenceData() {
        db.update("delete from chat_message");
        db.update("delete from chat_conversation");
        db.update("delete from user_voucher");
        db.update("delete from voucher");
        db.update("delete from user_block");
        db.update("delete from notification");
        db.update("delete from transaction_record");
        db.update("delete from balance");
        db.update("delete from address");
        db.update("insert into balance(user_id, personal_balance, business_balance, version) values(1, 100.00, 0.00, 0)");
        db.update("insert into balance(user_id, personal_balance, business_balance, version) values(3, 0.00, 0.00, 0)");
        db.update("insert into address(user_id, receiver_name, receiver_phone, province, city, detail_address, is_default) "
                + "values(1, '测试买家', '13800000000', '北京市', '北京市', '测试路1号', 1)");

        buyerToken = jwtUtils.createToken(1L, "buyer1", "USER");
        adminToken = jwtUtils.createToken(2L, "admin1", "ADMIN");
        sellerToken = jwtUtils.createToken(3L, "seller1", "OFFICIAL_SELLER");
    }

    @Test
    void uc21SellerAndAdminManageVoucherLifecycle() throws Exception {
        long sellerVoucherId = createVoucher("/api/voucher/seller", sellerToken, "UC21卖家优惠券", "10.00");

        mvc.perform(get("/api/voucher/seller/list")
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(sellerVoucherId));

        mvc.perform(post("/api/voucher/admin")
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voucherBody("越权平台券", "8.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        mvc.perform(put("/api/voucher/seller/{id}", sellerVoucherId)
                        .header("Authorization", bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voucherBody("UC21卖家优惠券-已修改", "15.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.discountAmount").value(15.0));

        mvc.perform(post("/api/voucher/seller/{id}/close", sellerVoucherId)
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertEquals(0, db.queryForObject(
                "select status from voucher where id = ?", Integer.class, sellerVoucherId));

        mvc.perform(delete("/api/voucher/seller/{id}", sellerVoucherId)
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertEquals(0, db.queryForObject(
                "select count(*) from voucher where id = ?", Integer.class, sellerVoucherId));

        long adminVoucherId = createVoucher("/api/voucher/admin", adminToken, "UC21平台优惠券", "8.00");

        mvc.perform(put("/api/voucher/admin/{id}", adminVoucherId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voucherBody("UC21平台优惠券-已修改", "12.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discountAmount").value(12.0));

        mvc.perform(get("/api/voucher/admin/list")
                        .param("name", "UC21平台优惠券-已修改")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(adminVoucherId));

        mvc.perform(post("/api/voucher/admin/{id}/close", adminVoucherId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertEquals(0, db.queryForObject(
                "select status from voucher where id = ?", Integer.class, adminVoucherId));

        mvc.perform(delete("/api/voucher/admin/{id}", adminVoucherId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void uc22BuyerClaimsAndUsesVoucherAtCheckout() throws Exception {
        long voucherId = createVoucher("/api/voucher/seller", sellerToken, "UC22结算优惠券", "10.00");

        mvc.perform(get("/api/voucher/list")
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(voucherId));

        mvc.perform(post("/api/voucher/{id}/claim", voucherId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(get("/api/voucher/my")
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(voucherId))
                .andExpect(jsonPath("$.data.records[0].myStatus").value(1));

        mvc.perform(get("/api/voucher/my/available/reasons")
                        .param("shopIds", "100")
                        .param("totalAmount", "50.00")
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value(org.hamcrest.Matchers.containsString("门槛不足")));

        mvc.perform(get("/api/voucher/my/available")
                        .param("shopIds", "100")
                        .param("totalAmount", "198.00")
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].id").value(voucherId));

        MvcResult orderCreated = mvc.perform(post("/api/order/create")
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1001,\"quantity\":2}],\"voucherId\":" + voucherId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalAmount").value(198.0))
                .andExpect(jsonPath("$.data.voucherDiscountAmount").value(10.0))
                .andExpect(jsonPath("$.data.payableAmount").value(188.0))
                .andReturn();
        long orderId = responseData(orderCreated).path("id").asLong();

        mvc.perform(post("/api/order/{id}/pay", orderId)
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payMode\":\"THIRD_PARTY\",\"payChannel\":\"WECHAT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(1));

        assertEquals(2, db.queryForObject(
                "select status from user_voucher where user_id = 1 and voucher_id = ?",
                Integer.class,
                voucherId));
        assertEquals(orderId, db.queryForObject(
                "select used_order_id from user_voucher where user_id = 1 and voucher_id = ?",
                Long.class,
                voucherId));
        assertEquals(new BigDecimal("188.00"), db.queryForObject(
                "select payable_amount from order_info where id = ?", BigDecimal.class, orderId));
        assertEquals(1, db.queryForObject(
                "select used_count from voucher where id = ?", Integer.class, voucherId));
    }

    @Test
    void uc23RechargeWalletBusinessAccountAndRecords() throws Exception {
        mvc.perform(post("/api/finance/recharge")
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.00,\"channel\":\"WECHAT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.personalBalance").value(150.0));

        mvc.perform(get("/api/finance/my-wallet/records")
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tradeType").value("RECHARGE"))
                .andExpect(jsonPath("$.data[0].amount").value(50.0));

        escrowSettlementService.changeBusinessBalance(
                3L,
                new BigDecimal("99.00"),
                5001L,
                "ORDER_SETTLEMENT",
                TransactionTradeTypeEnum.INCOME_BUSINESS,
                "UC23测试结算");

        mvc.perform(get("/api/finance/dashboard")
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.businessBalance").value(99.0));

        mvc.perform(get("/api/finance/business/records")
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orderId").value(5001))
                .andExpect(jsonPath("$.data[0].amount").value(99.0));
    }

    @Test
    void uc24ConversationAndMessageArePersisted() throws Exception {
        MvcResult created = mvc.perform(post("/api/chat/conversations")
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":3,\"sourceType\":\"PRODUCT\",\"sourceId\":1001}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sourceType").value("PRODUCT"))
                .andReturn();
        long conversationId = responseData(created).path("id").asLong();

        mvc.perform(get("/api/chat/conversations")
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(conversationId));

        mvc.perform(post("/api/chat/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"UC24 evidence message\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value("UC24 evidence message"))
                .andExpect(jsonPath("$.data.receiverUserId").value(3));

        mvc.perform(get("/api/chat/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("UC24 evidence message"))
                .andExpect(jsonPath("$.data[0].isRead").value(1));

        assertEquals(1, db.queryForObject(
                "select count(*) from chat_message where conversation_id = ?",
                Integer.class,
                conversationId));
        assertEquals(1, db.queryForObject(
                "select count(*) from notification where user_id = 3 and is_read = 0",
                Integer.class));
    }

    @Test
    void uc25NotificationReadAndRealtimePush() throws Exception {
        verifyHandshakeAccepts(buyerToken);

        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(RealtimeHandshakeInterceptor.USER_ID_ATTR, 1L);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);

        try {
            realtimeWebSocketHandler.afterConnectionEstablished(session);
            verify(session).sendMessage(any(TextMessage.class));

            clearInvocations(session);
            realtimeWebSocketHandler.handleMessage(
                    session,
                    new TextMessage("{\"eventType\":\"PING\"}"));
            verify(session).sendMessage(new TextMessage("{\"eventType\":\"PONG\"}"));

            clearInvocations(session);
            NotificationVO first = notificationService.createNotification(
                    1L,
                    "UC25实时通知",
                    "用于验证 WebSocket 推送",
                    "/order/101");
            verify(session, atLeastOnce()).sendMessage(any(TextMessage.class));

            mvc.perform(get("/api/notifications")
                            .header("Authorization", bearer(buyerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(first.getId()))
                    .andExpect(jsonPath("$.data[0].isRead").value(0));

            mvc.perform(post("/api/notifications/{id}/read", first.getId())
                            .header("Authorization", bearer(buyerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
            assertEquals(1, db.queryForObject(
                    "select is_read from notification where id = ?",
                    Integer.class,
                    first.getId()));

            notificationService.createNotification(1L, "UC25第二条通知", "验证全部已读");
            mvc.perform(post("/api/notifications/read-all")
                            .header("Authorization", bearer(buyerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
            assertEquals(0, db.queryForObject(
                    "select count(*) from notification where user_id = 1 and is_read = 0",
                    Integer.class));
        } finally {
            realtimeWebSocketHandler.afterConnectionClosed(session, CloseStatus.NORMAL);
        }
    }

    private long createVoucher(String path, String token, String name, String discountAmount) throws Exception {
        MvcResult result = mvc.perform(post(path)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voucherBody(name, discountAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return responseData(result).path("id").asLong();
    }

    private String voucherBody(String name, String discountAmount) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("type", 1);
        body.put("discountAmount", new BigDecimal(discountAmount));
        body.put("minAmount", new BigDecimal("100.00"));
        body.put("noThreshold", false);
        body.put("totalCount", 50);
        body.put("grabStartTime", now.minusDays(1));
        body.put("grabEndTime", now.plusDays(2));
        body.put("startTime", now.minusDays(1));
        body.put("endTime", now.plusDays(7));
        return objectMapper.writeValueAsString(body);
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private void verifyHandshakeAccepts(String token) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler handler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();
        when(request.getURI()).thenReturn(URI.create("http://localhost/ws/realtime?token=" + token));

        boolean accepted = realtimeHandshakeInterceptor.beforeHandshake(
                request,
                response,
                handler,
                attributes);

        assertTrue(accepted);
        assertEquals(1L, attributes.get(RealtimeHandshakeInterceptor.USER_ID_ATTR));
        verify(response, org.mockito.Mockito.never()).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

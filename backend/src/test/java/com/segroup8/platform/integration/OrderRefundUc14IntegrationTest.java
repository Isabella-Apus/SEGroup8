package com.segroup8.platform.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.OrderStatusEnum;
import com.segroup8.platform.common.RefundDecisionSourceEnum;
import com.segroup8.platform.common.RefundStatusEnum;
import com.segroup8.platform.common.TransactionTradeTypeEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.OrderRefundApplyRequest;
import com.segroup8.platform.entity.OrderAfterSaleLog;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.TransactionRecord;
import com.segroup8.platform.mapper.OrderAfterSaleLogMapper;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.TransactionRecordMapper;
import com.segroup8.platform.service.OrderService;
import com.segroup8.platform.testsupport.DomainCTestTags;
import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.AfterEach;
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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Sql(scripts = "/integration/uc14-refund-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Tag(DomainCTestTags.DOMAIN_C)
@Tag(DomainCTestTags.UC14)
class OrderRefundUc14IntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.6")
            .withDatabaseName("segroup8_uc14")
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

    @Autowired private OrderService orderService;
    @Autowired private OrderInfoMapper orderInfoMapper;
    @Autowired private TransactionRecordMapper transactionRecordMapper;
    @Autowired private OrderAfterSaleLogMapper orderAfterSaleLogMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtils jwtUtils;

    private String sellerToken;

    @BeforeEach
    void createTokens() {
        sellerToken = jwtUtils.createToken(10L, "uc14_seller", "OFFICIAL_SELLER");
    }

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void buyerApply_sellerReject_reapply_adminApprove_recordsCompleteOrderedAuditLog() {
        UserContext.setUserId(1L);
        orderService.refundMyOrder(301L, request("RETURN_REFUND", "商品存在瑕疵"));

        UserContext.setUserId(10L);
        orderService.rejectRefundBySeller(301L);
        assertEquals(RefundStatusEnum.REJECTED.getCode(), orderInfoMapper.selectById(301L).getRefundStatus());

        UserContext.setUserId(1L);
        orderService.refundMyOrder(301L, request("RETURN_REFUND", "补充凭证后再次申请"));
        orderService.approveRefundByAdmin(301L, 2L, "平台仲裁同意");

        OrderInfo order = orderInfoMapper.selectById(301L);
        assertEquals(RefundStatusEnum.APPROVED.getCode(), order.getRefundStatus());
        assertEquals(OrderStatusEnum.CLOSED.getCode(), order.getOrderStatus());
        assertEquals(RefundDecisionSourceEnum.ADMIN.name(), order.getRefundDecisionSource());

        List<OrderAfterSaleLog> logs = logs(301L);
        assertThat(logs).extracting(OrderAfterSaleLog::getAction)
                .containsExactly("APPLY", "REJECT", "APPLY", "APPROVE");
        assertThat(logs).extracting(OrderAfterSaleLog::getOperatorUserId)
                .containsExactly(1L, 10L, 1L, 2L);
        assertThat(logs).extracting(OrderAfterSaleLog::getOperatorRole)
                .containsExactly("BUYER", "SELLER", "BUYER", "ADMIN");
        assertThat(logs).extracting(OrderAfterSaleLog::getRemark)
                .allMatch(remark -> remark != null && !remark.isBlank());
        assertThat(logs).extracting(OrderAfterSaleLog::getCreateTime).doesNotContainNull();
    }

    @Test
    void adminReject_recordsDecisionWithoutRefundSideEffects() {
        UserContext.setUserId(1L);
        orderService.refundMyOrder(301L, request("RETURN_REFUND", "申请管理员仲裁"));
        orderService.rejectRefundByAdmin(301L, 2L, "凭证不足");

        OrderInfo order = orderInfoMapper.selectById(301L);
        assertEquals(RefundStatusEnum.REJECTED.getCode(), order.getRefundStatus());
        assertEquals(OrderStatusEnum.SHIPPED.getCode(), order.getOrderStatus());
        assertEquals(RefundDecisionSourceEnum.ADMIN.name(), order.getRefundDecisionSource());
        assertEquals(0, refundLedger(301L).size());
        assertThat(logs(301L)).extracting(OrderAfterSaleLog::getAction)
                .containsExactly("APPLY", "REJECT");
    }

    @Test
    void pendingShipOnlyRefund_autoApproves_refundsBuyer_andRestoresStock() {
        int stockBefore = stock(402L);
        UserContext.setUserId(1L);
        orderService.refundMyOrder(302L, request("ONLY_REFUND", "未发货取消"));

        OrderInfo order = orderInfoMapper.selectById(302L);
        assertEquals(RefundStatusEnum.APPROVED.getCode(), order.getRefundStatus());
        assertEquals(OrderStatusEnum.CLOSED.getCode(), order.getOrderStatus());
        assertEquals(RefundDecisionSourceEnum.SYSTEM.name(), order.getRefundDecisionSource());
        assertEquals(new BigDecimal("50.00"), personalBalance(1L));
        assertEquals(stockBefore + 1, stock(402L));
        assertThat(logs(302L)).extracting(OrderAfterSaleLog::getOperatorRole)
                .containsExactly("BUYER", "ADMIN");
    }

    @Test
    void refundModes_enforceOnlyRefundAndReturnRefundBoundaries_withoutMutatingRejectedRequests() {
        UserContext.setUserId(1L);
        assertThrows(BusinessException.class,
                () -> orderService.refundMyOrder(301L, request("UNSUPPORTED", "不支持的退款方式")));
        assertThrows(BusinessException.class,
                () -> orderService.refundMyOrder(304L, request("RETURN_REFUND", "运输中不能退货退款")));
        assertEquals(RefundStatusEnum.NONE.getCode(), orderInfoMapper.selectById(301L).getRefundStatus());
        assertEquals(RefundStatusEnum.NONE.getCode(), orderInfoMapper.selectById(304L).getRefundStatus());

        int stockBefore = stock(401L);
        orderService.refundMyOrder(301L, request("RETURN_REFUND", "到货后退货退款"));
        UserContext.setUserId(10L);
        orderService.approveRefundBySeller(301L);
        assertEquals(stockBefore, stock(401L));
    }

    @Test
    void buyerSellerAndAdminPermissions_rejectUnrelatedActors_andLeaveStateUnchanged() throws Exception {
        UserContext.setUserId(3L);
        assertThrows(BusinessException.class,
                () -> orderService.refundMyOrder(301L, request("RETURN_REFUND", "越权申请")));

        UserContext.setUserId(1L);
        orderService.refundMyOrder(301L, request("RETURN_REFUND", "合法申请"));
        UserContext.setUserId(11L);
        assertThrows(BusinessException.class, () -> orderService.approveRefundBySeller(301L));

        mockMvc.perform(post("/api/admin/orders/301/refund/approve")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType("application/json")
                        .content("{\"remark\":\"伪造管理员审批\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
        assertEquals(RefundStatusEnum.PROCESSING.getCode(), orderInfoMapper.selectById(301L).getRefundStatus());
        assertEquals(0, refundLedger(301L).size());
    }

    @Test
    void settledRefund_returnsBuyerFunds_debitsSeller_andKeepsRefundLedgerConserved() {
        orderService.autoConfirmReceiveForSystem(301L);
        assertEquals(new BigDecimal("120.00"), businessBalance(10L));

        UserContext.setUserId(1L);
        orderService.refundMyOrder(301L, request("RETURN_REFUND", "收货后发现质量问题"));
        orderService.approveRefundByAdmin(301L, 2L, "结算后退款");

        assertEquals(new BigDecimal("120.00"), personalBalance(1L));
        assertEquals(new BigDecimal("0.00"), businessBalance(10L));
        List<TransactionRecord> refundRecords = refundLedger(301L);
        assertEquals(2, refundRecords.size());
        assertThat(refundRecords).extracting(TransactionRecord::getTradeType)
                .containsOnly(TransactionTradeTypeEnum.REFUND_BACKFLOW.getCode());
        BigDecimal refundNet = refundRecords.stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, BigDecimal.ZERO.compareTo(refundNet));
    }

    @Test
    void concurrentSellerAndAdminApproval_producesExactlyOneRefundAndOneApprovalLog() throws Exception {
        UserContext.setUserId(1L);
        orderService.refundMyOrder(301L, request("RETURN_REFUND", "并发审核"));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> seller = pool.submit((Callable<Void>) () -> {
                UserContext.setUserId(10L);
                try {
                    orderService.approveRefundBySeller(301L);
                    return null;
                } finally {
                    UserContext.clear();
                }
            });
            Future<?> admin = pool.submit((Callable<Void>) () -> {
                orderService.approveRefundByAdmin(301L, 2L, "管理员并发审核");
                return null;
            });
            int successes = 0;
            for (Future<?> future : List.of(seller, admin)) {
                try {
                    future.get();
                    successes++;
                } catch (ExecutionException expected) {
                    // The optimistic version condition rejects the losing decision.
                }
            }
            assertEquals(1, successes);
        } finally {
            pool.shutdownNow();
            UserContext.clear();
        }
        assertEquals(RefundStatusEnum.APPROVED.getCode(), orderInfoMapper.selectById(301L).getRefundStatus());
        assertEquals(1, refundLedger(301L).size());
        assertEquals(1, logs(301L).stream().filter(log -> "APPROVE".equals(log.getAction())).count());
    }

    private List<OrderAfterSaleLog> logs(Long orderId) {
        return orderAfterSaleLogMapper.selectList(new LambdaQueryWrapper<OrderAfterSaleLog>()
                .eq(OrderAfterSaleLog::getOrderId, orderId)
                .orderByAsc(OrderAfterSaleLog::getCreateTime)
                .orderByAsc(OrderAfterSaleLog::getId));
    }

    private List<TransactionRecord> refundLedger(Long orderId) {
        return transactionRecordMapper.selectList(new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getOrderId, orderId)
                .eq(TransactionRecord::getTradeType, TransactionTradeTypeEnum.REFUND_BACKFLOW.getCode())
                .orderByAsc(TransactionRecord::getId));
    }

    private int stock(Long productId) {
        return jdbcTemplate.queryForObject("select stock from product where id = ?", Integer.class, productId);
    }

    private BigDecimal personalBalance(Long userId) {
        return jdbcTemplate.queryForObject(
                "select personal_balance from balance where user_id = ?", BigDecimal.class, userId);
    }

    private BigDecimal businessBalance(Long userId) {
        return jdbcTemplate.queryForObject(
                "select business_balance from balance where user_id = ?", BigDecimal.class, userId);
    }

    private OrderRefundApplyRequest request(String mode, String reason) {
        OrderRefundApplyRequest request = new OrderRefundApplyRequest();
        request.setRefundMode(mode);
        request.setReason(reason);
        return request;
    }
}

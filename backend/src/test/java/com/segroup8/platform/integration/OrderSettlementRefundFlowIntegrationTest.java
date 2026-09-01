package com.segroup8.platform.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.segroup8.platform.common.OrderStatusEnum;
import com.segroup8.platform.common.RefundDecisionSourceEnum;
import com.segroup8.platform.common.RefundStatusEnum;
import com.segroup8.platform.common.TransactionTradeTypeEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.OrderRefundApplyRequest;
import com.segroup8.platform.entity.Balance;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.TransactionRecord;
import com.segroup8.platform.mapper.BalanceMapper;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.TransactionRecordMapper;
import com.segroup8.platform.schedule.OrderAutoConfirmScheduler;
import com.segroup8.platform.service.OrderService;
import com.segroup8.platform.testsupport.DomainCTestTags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.messaging.event-notifications-enabled=true")
@Sql(scripts = "classpath:integration/full-flow-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Tag(DomainCTestTags.DOMAIN_C)
@Tag(DomainCTestTags.PLATFORM)
class OrderSettlementRefundFlowIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderAutoConfirmScheduler orderAutoConfirmScheduler;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private BalanceMapper balanceMapper;

    @Autowired
    private TransactionRecordMapper transactionRecordMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanOutbox() {
        jdbcTemplate.update("delete from outbox_event");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void deliveredToAutoConfirmShouldSettleAndKeepAccountIsolation() {
        orderService.autoConfirmReceiveForSystem(301L);

        OrderInfo order = orderInfoMapper.selectById(301L);
        assertEquals(OrderStatusEnum.RECEIVED.getCode(), order.getOrderStatus());
        assertNotNull(order.getAfterSalesDeadline());

        Balance sellerBalance = balanceMapper.selectById(10L);
        assertEquals(new BigDecimal("120.00"), sellerBalance.getBusinessBalance());
        assertEquals(new BigDecimal("0.00"), sellerBalance.getPersonalBalance());

        List<TransactionRecord> sellerRecords = transactionRecordMapper
                .selectList(new LambdaQueryWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getUserId, 10L)
                        .eq(TransactionRecord::getOrderId, 301L)
                        .orderByAsc(TransactionRecord::getId));
        assertEquals(1, sellerRecords.size());
        assertEquals("BUSINESS", sellerRecords.get(0).getAccountType());
        assertEquals(TransactionTradeTypeEnum.INCOME_BUSINESS.getCode(), sellerRecords.get(0).getTradeType());
        assertEquals(new BigDecimal("120.00"), sellerRecords.get(0).getAmount());
        assertEquals(2, jdbcTemplate.queryForObject(
                "select count(*) from outbox_event where event_type='OrderStatusChanged.v1'", Integer.class));
    }

    @Test
    void refundSplitShouldHandleOnlyRefundAndTimeoutAutoRefund() {
        UserContext.setUserId(1L);

        OrderRefundApplyRequest onlyRefundReq = new OrderRefundApplyRequest();
        onlyRefundReq.setRefundMode("ONLY_REFUND");
        onlyRefundReq.setReason("未发货改主意");
        orderService.refundMyOrder(302L, onlyRefundReq);

        OrderInfo onlyRefundOrder = orderInfoMapper.selectById(302L);
        assertEquals(RefundStatusEnum.APPROVED.getCode(), onlyRefundOrder.getRefundStatus());
        assertEquals(OrderStatusEnum.CLOSED.getCode(), onlyRefundOrder.getOrderStatus());

        orderAutoConfirmScheduler.autoApproveTimeoutRefundOrders();

        OrderInfo timeoutRefundOrder = orderInfoMapper.selectById(303L);
        assertEquals(RefundStatusEnum.APPROVED.getCode(), timeoutRefundOrder.getRefundStatus());
        assertEquals(OrderStatusEnum.CLOSED.getCode(), timeoutRefundOrder.getOrderStatus());
        assertEquals(RefundDecisionSourceEnum.SYSTEM.name(), timeoutRefundOrder.getRefundDecisionSource());

        Balance buyerBalance = balanceMapper.selectById(1L);
        assertEquals(new BigDecimal("120.00"), buyerBalance.getPersonalBalance());
        assertEquals(new BigDecimal("0.00"), buyerBalance.getBusinessBalance());

        List<TransactionRecord> buyerRecords = transactionRecordMapper
                .selectList(new LambdaQueryWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getUserId, 1L)
                        .orderByAsc(TransactionRecord::getId));
        assertEquals(2, buyerRecords.size());
        assertEquals(TransactionTradeTypeEnum.REFUND_BACKFLOW.getCode(), buyerRecords.get(0).getTradeType());
        assertEquals(TransactionTradeTypeEnum.REFUND_BACKFLOW.getCode(), buyerRecords.get(1).getTradeType());
        Integer refundEvents = jdbcTemplate.queryForObject(
                "select count(*) from outbox_event where event_type='RefundCompleted.v1'", Integer.class);
        org.junit.jupiter.api.Assertions.assertTrue(refundEvents != null && refundEvents >= 2);
    }
}

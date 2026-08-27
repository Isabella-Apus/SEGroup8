package com.segroup8.platform.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.segroup8.platform.common.OrderStatusEnum;
import com.segroup8.platform.common.RefundDecisionSourceEnum;
import com.segroup8.platform.common.RefundStatusEnum;
import com.segroup8.platform.common.TransactionTradeTypeEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.OrderRefundApplyRequest;
import com.segroup8.platform.entity.Balance;
import com.segroup8.platform.entity.OrderAfterSaleLog;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.TransactionRecord;
import com.segroup8.platform.mapper.BalanceMapper;
import com.segroup8.platform.mapper.OrderAfterSaleLogMapper;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.TransactionRecordMapper;
import com.segroup8.platform.service.OrderService;
import com.segroup8.platform.testsupport.DomainCTestTags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "classpath:integration/full-flow-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Tag(DomainCTestTags.DOMAIN_C)
@Tag(DomainCTestTags.UC14)
class OrderRefundUc14IntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private BalanceMapper balanceMapper;

    @Autowired
    private TransactionRecordMapper transactionRecordMapper;

    @Autowired
    private OrderAfterSaleLogMapper orderAfterSaleLogMapper;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void buyerApply_thenSellerReject_thenReapplyAndAdminApprove_recordsOrderedLogs() {
        UserContext.setUserId(1L);
        OrderRefundApplyRequest request = request("RETURN_REFUND", "商品存在瑕疵");
        orderService.refundMyOrder(301L, request);

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
        List<OrderAfterSaleLog> logs = orderAfterSaleLogMapper.selectList(new LambdaQueryWrapper<OrderAfterSaleLog>()
                .eq(OrderAfterSaleLog::getOrderId, 301L)
                .orderByAsc(OrderAfterSaleLog::getCreateTime)
                .orderByAsc(OrderAfterSaleLog::getId));
        assertEquals(4, logs.size());
        assertEquals("APPLY", logs.get(0).getAction());
        assertEquals("REJECT", logs.get(1).getAction());
        assertEquals("APPLY", logs.get(2).getAction());
        assertEquals("APPROVE", logs.get(3).getAction());
    }

    @Test
    void pendingShipOnlyRefund_autoApprovesAndRestoresStock() {
        UserContext.setUserId(1L);
        OrderRefundApplyRequest request = request("ONLY_REFUND", "未发货取消");
        orderService.refundMyOrder(302L, request);

        OrderInfo order = orderInfoMapper.selectById(302L);
        assertEquals(RefundStatusEnum.APPROVED.getCode(), order.getRefundStatus());
        assertEquals(OrderStatusEnum.CLOSED.getCode(), order.getOrderStatus());
        assertEquals(RefundDecisionSourceEnum.SYSTEM.name(), order.getRefundDecisionSource());
        assertEquals(new BigDecimal("50.00"), balanceMapper.selectById(1L).getPersonalBalance());
        assertEquals(2, orderAfterSaleLogMapper.selectList(new LambdaQueryWrapper<OrderAfterSaleLog>()
                .eq(OrderAfterSaleLog::getOrderId, 302L)).size());
    }

    @Test
    void refundModes_areValidated_andReturnRefundRequiresEligibleState() {
        UserContext.setUserId(1L);
        assertThrows(RuntimeException.class,
                () -> orderService.refundMyOrder(301L, request("UNSUPPORTED", "不支持的退款方式")));
        assertEquals(RefundStatusEnum.NONE.getCode(), orderInfoMapper.selectById(301L).getRefundStatus());

        orderService.refundMyOrder(301L, request("RETURN_REFUND", "申请退货退款"));
        assertEquals(RefundStatusEnum.PROCESSING.getCode(), orderInfoMapper.selectById(301L).getRefundStatus());
    }

    @Test
    void settledRefund_returnsBuyerFundsAndDebitsSellerExactlyOnce() {
        orderService.autoConfirmReceiveForSystem(301L);
        UserContext.setUserId(1L);
        orderService.refundMyOrder(301L, request("RETURN_REFUND", "收货后发现质量问题"));
        orderService.approveRefundByAdmin(301L, 2L, "结算后退款");

        Balance buyer = balanceMapper.selectById(1L);
        Balance seller = balanceMapper.selectById(10L);
        assertEquals(new BigDecimal("120.00"), buyer.getPersonalBalance());
        assertEquals(new BigDecimal("0.00"), seller.getBusinessBalance());
        List<TransactionRecord> records = transactionRecordMapper.selectList(new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getOrderId, 301L)
                .orderByAsc(TransactionRecord::getId));
        assertEquals(3, records.size());
        assertEquals(TransactionTradeTypeEnum.REFUND_BACKFLOW.getCode(), records.get(1).getTradeType());
        assertEquals(TransactionTradeTypeEnum.REFUND_BACKFLOW.getCode(), records.get(2).getTradeType());
    }

    @Test
    void concurrentSellerAndAdminApproval_hasAtMostOneRefundSideEffect() throws Exception {
        UserContext.setUserId(1L);
        orderService.refundMyOrder(301L, request("RETURN_REFUND", "并发审核"));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> seller = pool.submit((Callable<Void>) () -> {
                UserContext.setUserId(10L);
                orderService.approveRefundBySeller(301L);
                return null;
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
                    // optimistic version condition rejects the loser
                }
            }
            assertEquals(1, successes);
        } finally {
            pool.shutdownNow();
            UserContext.clear();
        }
        assertEquals(RefundStatusEnum.APPROVED.getCode(), orderInfoMapper.selectById(301L).getRefundStatus());
        assertEquals(1, transactionRecordMapper.selectList(new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getOrderId, 301L)
                .eq(TransactionRecord::getTradeType, TransactionTradeTypeEnum.REFUND_BACKFLOW.getCode())).size());
    }

    private OrderRefundApplyRequest request(String mode, String reason) {
        OrderRefundApplyRequest request = new OrderRefundApplyRequest();
        request.setRefundMode(mode);
        request.setReason(reason);
        return request;
    }
}

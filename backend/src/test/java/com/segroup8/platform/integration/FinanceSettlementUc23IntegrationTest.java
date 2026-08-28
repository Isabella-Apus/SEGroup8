package com.segroup8.platform.integration;

import com.segroup8.platform.common.TransactionTradeTypeEnum;
import com.segroup8.platform.service.settlement.EscrowSettlementService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("DOMAIN_E")
@Tag("UC23")
class FinanceSettlementUc23IntegrationTest extends DomainEIntegrationTestBase {

    @Autowired
    private EscrowSettlementService escrowSettlementService;

    @Test
    void walletInitializesRechargesAndReturnsOnlyTheOwnersPersonalRecords() throws Exception {
        db.update("delete from balance where user_id = 1");

        mvc.perform(get("/api/finance/dashboard")
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.personalBalance").value(0.0))
                .andExpect(jsonPath("$.data.businessBalance").value(0.0));
        assertEquals(1, count("select count(*) from balance where user_id = 1"));

        mvc.perform(post("/api/finance/recharge")
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.00,\"channel\":\"WECHAT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.personalBalance").value(50.0))
                .andExpect(jsonPath("$.data.businessBalance").value(0.0));

        mvc.perform(get("/api/finance/my-wallet/records")
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].accountType").value("PERSONAL"))
                .andExpect(jsonPath("$.data[0].tradeType").value("RECHARGE"))
                .andExpect(jsonPath("$.data[0].amount").value(50.0));

        mvc.perform(get("/api/finance/business/records")
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        assertEquals(new BigDecimal("50.00"), decimal(
                "select personal_balance from balance where user_id = 1"));
        assertEquals(new BigDecimal("0.00"), decimal(
                "select business_balance from balance where user_id = 1"));
        assertEquals(1, count("select count(*) from transaction_record "
                + "where user_id = 1 and account_type = 'PERSONAL' and trade_type = 'RECHARGE'"));
    }

    @Test
    void confirmedNewProductOrderSettlesOnceIntoTheSellerBusinessAccount() throws Exception {
        long orderId = createPaidAndShippedNewProductOrder();

        mvc.perform(post("/api/order/{id}/confirm-receive", orderId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(get("/api/finance/dashboard")
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.personalBalance").value(0.0))
                .andExpect(jsonPath("$.data.businessBalance").value(99.0));
        mvc.perform(get("/api/finance/business/records")
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orderId").value(orderId))
                .andExpect(jsonPath("$.data[0].accountType").value("BUSINESS"))
                .andExpect(jsonPath("$.data[0].tradeType").value("INCOME_BUSINESS"))
                .andExpect(jsonPath("$.data[0].amount").value(99.0));

        mvc.perform(post("/api/order/{id}/confirm-receive", orderId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        assertEquals(new BigDecimal("99.00"), decimal(
                "select business_balance from balance where user_id = 3"));
        assertEquals(1, count("select count(*) from transaction_record "
                + "where user_id = 3 and order_id = " + orderId + " and account_type = 'BUSINESS'"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void transactionRecordFailureRollsBackTheBalanceUpdate() {
        assertThrows(RuntimeException.class, () -> escrowSettlementService.changePersonalBalance(
                1L,
                new BigDecimal("10.00"),
                23001L,
                "UC23_ROLLBACK",
                TransactionTradeTypeEnum.RECHARGE,
                "x".repeat(300)));

        assertEquals(new BigDecimal("100.00"), decimal(
                "select personal_balance from balance where user_id = 1"));
        assertEquals(0, count("select count(*) from transaction_record where order_id = 23001"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentPersonalCreditsDoNotLoseMoneyOrLedgerRows() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<BigDecimal> first = concurrentCredit(ready, start, 23011L);
            Callable<BigDecimal> second = concurrentCredit(ready, start, 23012L);
            Future<BigDecimal> firstResult = pool.submit(first);
            Future<BigDecimal> secondResult = pool.submit(second);
            ready.await();
            start.countDown();
            List.of(firstResult, secondResult).forEach(result -> {
                try {
                    result.get();
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });
        } finally {
            pool.shutdownNow();
        }

        assertEquals(new BigDecimal("150.00"), decimal(
                "select personal_balance from balance where user_id = 1"));
        assertEquals(2, count("select count(*) from transaction_record "
                + "where order_id in (23011, 23012) and account_type = 'PERSONAL'"));
    }

    @Test
    void refundMovesTheSettledAmountBackWithoutChangingTheCombinedBalance() {
        escrowSettlementService.changeBusinessBalance(
                3L, new BigDecimal("40.00"), 23021L, "SETTLEMENT",
                TransactionTradeTypeEnum.INCOME_BUSINESS, "订单结算");
        BigDecimal combinedBeforeRefund = combinedBuyerAndSellerBalance();

        escrowSettlementService.changeBusinessBalance(
                3L, new BigDecimal("-40.00"), 23021L, "REFUND_SELLER_DEDUCT",
                TransactionTradeTypeEnum.REFUND_BACKFLOW, "结算后退款");
        escrowSettlementService.changePersonalBalance(
                1L, new BigDecimal("40.00"), 23021L, "REFUND_BUYER_BACKFLOW",
                TransactionTradeTypeEnum.REFUND_BACKFLOW, "结算后退款");

        assertEquals(combinedBeforeRefund, combinedBuyerAndSellerBalance());
        assertEquals(new BigDecimal("0.00"), decimal(
                "select business_balance from balance where user_id = 3"));
        assertEquals(new BigDecimal("140.00"), decimal(
                "select personal_balance from balance where user_id = 1"));
        assertEquals(new BigDecimal("0.00"), decimal(
                "select coalesce(sum(amount), 0) from transaction_record "
                        + "where order_id = 23021 and trade_type = 'REFUND_BACKFLOW'"));
    }

    private long createPaidAndShippedNewProductOrder() throws Exception {
        var created = mvc.perform(post("/api/order/create")
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1001,\"quantity\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.payableAmount").value(99.0))
                .andReturn();
        long orderId = responseData(created).path("id").asLong();

        mvc.perform(post("/api/order/{id}/pay", orderId)
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payMode\":\"THIRD_PARTY\",\"payChannel\":\"WECHAT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(post("/api/order/{id}/ship", orderId)
                        .header("Authorization", bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originProvince\":\"北京市\",\"originCity\":\"北京市\","
                                + "\"originDetail\":\"UC23测试仓库\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        return orderId;
    }

    private Callable<BigDecimal> concurrentCredit(
            CountDownLatch ready,
            CountDownLatch start,
            Long orderId) {
        return () -> {
            ready.countDown();
            start.await();
            return escrowSettlementService.changePersonalBalance(
                    1L,
                    new BigDecimal("25.00"),
                    orderId,
                    "UC23_CONCURRENT",
                    TransactionTradeTypeEnum.RECHARGE,
                    "并发充值");
        };
    }

    private BigDecimal combinedBuyerAndSellerBalance() {
        return decimal("select personal_balance from balance where user_id = 1")
                .add(decimal("select business_balance from balance where user_id = 3"));
    }

    private BigDecimal decimal(String sql) {
        return db.queryForObject(sql, BigDecimal.class);
    }

    private int count(String sql) {
        return db.queryForObject(sql, Integer.class);
    }
}

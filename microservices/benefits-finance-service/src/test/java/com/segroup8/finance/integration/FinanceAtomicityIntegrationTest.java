package com.segroup8.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.segroup8.finance.ApiModels.DebitRequest;
import com.segroup8.finance.ApiModels.RefundRequest;
import com.segroup8.finance.ApiModels.SettlementRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(classes=BenefitsFinanceApplication.class)
class FinanceAtomicityIntegrationTest {
    @Autowired FinanceService finance;
    @Autowired JdbcTemplate db;

    @BeforeEach
    void reset() {
        db.update("delete from outbox_event");
        db.update("delete from transaction_record");
        db.update("delete from payment_request");
        db.update("delete from balance");
        db.update("insert into balance(user_id,personal_balance,business_balance,version) values(101,100,0,0)");
    }

    @Test
    void debitRefundAndSettlementKeepBalanceEqualToLedger() {
        var debit = finance.debit(new DebitRequest("pay-1", 9001L, 101L, new BigDecimal("60.00")));
        var repeated = finance.debit(new DebitRequest("pay-1", 9001L, 101L, new BigDecimal("60.00")));
        assertThat(repeated.transactionId()).isEqualTo(debit.transactionId());
        finance.refund(new RefundRequest("refund-1", "pay-1", 9001L, 101L, new BigDecimal("20.00")));
        finance.settlement(new SettlementRequest(9001L, 7L, new BigDecimal("40.00")));
        finance.settlement(new SettlementRequest(9001L, 7L, new BigDecimal("40.00")));

        assertThat(personal(101)).isEqualByComparingTo("60.00");
        assertThat(business(7)).isEqualByComparingTo("40.00");
        assertThat(db.queryForObject("select count(*) from transaction_record", Integer.class)).isEqualTo(3);
        assertThat(db.queryForObject("select count(*) from outbox_event", Integer.class)).isEqualTo(3);
        BigDecimal personalDelta = db.queryForObject("select sum(amount) from transaction_record where user_id=101", BigDecimal.class);
        assertThat(new BigDecimal("100.00").add(personalDelta)).isEqualByComparingTo(personal(101));
    }

    @Test
    void insufficientDebitRollsBackRequestBalanceLedgerAndEvent() {
        assertThatThrownBy(() -> finance.debit(new DebitRequest("pay-no-money", 9002L, 101L, new BigDecimal("101.00"))))
                .hasMessageContaining("余额不足");
        assertThat(personal(101)).isEqualByComparingTo("100.00");
        assertThat(db.queryForObject("select count(*) from payment_request where request_id='pay-no-money'", Integer.class)).isZero();
        assertThat(db.queryForObject("select count(*) from transaction_record", Integer.class)).isZero();
        assertThat(db.queryForObject("select count(*) from outbox_event", Integer.class)).isZero();
    }

    @Test
    void concurrentSettlementCreditsSellerOnlyOnce() throws Exception {
        var pool = Executors.newFixedThreadPool(6);
        try {
            List<Callable<String>> calls = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                calls.add(() -> finance.settlement(new SettlementRequest(9900L, 7L, new BigDecimal("35.00"))).transactionId());
            }
            var results = pool.invokeAll(calls).stream().map(future -> {
                try { return future.get(); } catch (Exception error) { throw new RuntimeException(error); }
            }).toList();
            assertThat(results).containsOnly(results.get(0));
            assertThat(business(7)).isEqualByComparingTo("35.00");
            assertThat(db.queryForObject("select count(*) from transaction_record where trade_type='SETTLEMENT'", Integer.class)).isOne();
        } finally {
            pool.shutdownNow();
        }
    }

    private BigDecimal personal(long userId) {
        return db.queryForObject("select personal_balance from balance where user_id=?", BigDecimal.class, userId);
    }
    private BigDecimal business(long userId) {
        return db.queryForObject("select business_balance from balance where user_id=?", BigDecimal.class, userId);
    }
}

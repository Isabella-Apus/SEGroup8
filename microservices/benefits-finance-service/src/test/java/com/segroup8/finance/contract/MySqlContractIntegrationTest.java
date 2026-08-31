package com.segroup8.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.segroup8.finance.ApiModels.DebitRequest;
import com.segroup8.finance.ApiModels.RefundRequest;
import com.segroup8.finance.ApiModels.SettlementRequest;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes=BenefitsFinanceApplication.class)
class MySqlContractIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.6")
            .withDatabaseName("benefits_finance_db")
            .withUsername("benefits_finance_app")
            .withPassword("test-only");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) throws Exception {
        MYSQL.start();
        var principals = MYSQL.execInContainer("mysql", "--protocol=socket", "-uroot", "-p" + MYSQL.getPassword(),
                "-e", "create user if not exists 'benefits_finance_migrator'@'%' identified by 'migrator-only'; "
                        + "revoke all privileges, grant option from 'benefits_finance_app'@'%'; "
                        + "grant select,insert,update,delete on benefits_finance_db.* to 'benefits_finance_app'@'%'; "
                        + "grant all privileges on benefits_finance_db.* to 'benefits_finance_migrator'@'%'; flush privileges;");
        if (principals.getExitCode() != 0) throw new IllegalStateException(principals.getStderr());
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
        properties.add("spring.flyway.user", () -> "benefits_finance_migrator");
        properties.add("spring.flyway.password", () -> "migrator-only");
    }

    @Autowired JdbcTemplate db;
    @Autowired FinanceService finance;

    @BeforeEach
    void reset() {
        db.update("delete from outbox_event");
        db.update("delete from idempotency_record");
        db.update("delete from transaction_record");
        db.update("delete from payment_request");
        db.update("delete from checkout_quote");
        db.update("delete from user_voucher");
        db.update("delete from voucher");
        db.update("delete from balance");
        db.update("insert into balance(user_id,personal_balance,business_balance,version) values(101,100,0,0)");
    }

    @Test
    void flywayUsesMigratorWhileRuntimeApplicationUserHasOnlyDmlAndNoCrossSchemaAccess() throws Exception {
        assertThat(db.queryForObject("select count(*) from information_schema.tables where table_schema=database() "
                + "and table_name in ('voucher','user_voucher','balance','transaction_record','checkout_quote',"
                + "'payment_request','idempotency_record','outbox_event')", Integer.class)).isEqualTo(8);
        assertThatThrownBy(() -> db.update(
                "insert into balance(user_id,personal_balance,business_balance,version) values(2,-0.01,0,0)"))
                .isInstanceOf(DataAccessException.class);
        assertThat(db.queryForObject("select current_user()", String.class)).startsWith("benefits_finance_app@");
        assertThatThrownBy(() -> db.execute("create table runtime_must_not_ddl(id bigint)"))
                .isInstanceOf(DataAccessException.class);
        try (Connection migrator = DriverManager.getConnection(MYSQL.getJdbcUrl(), "benefits_finance_migrator", "migrator-only")) {
            migrator.createStatement().execute("create table migrator_proof(id bigint primary key)");
            migrator.createStatement().execute("drop table migrator_proof");
        }

        var setup = MYSQL.execInContainer("mysql", "--protocol=socket", "-uroot", "-p" + MYSQL.getPassword(),
                "-e", "create database if not exists order_db; "
                        + "create table if not exists order_db.order_info(id bigint primary key, status int not null); "
                        + "insert into order_db.order_info(id,status) values(1,0) "
                        + "on duplicate key update status=values(status)");
        assertThat(setup.getExitCode()).as(setup.getStderr()).isZero();
        assertThatThrownBy(() -> db.update("update order_db.order_info set status=1 where id=1"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void mysqlDebitRefundSettlementAndFailuresPreserveAtomicLedger() {
        var debit = finance.debit(new DebitRequest("mysql-pay-1", 9001L, 101L, new BigDecimal("60.00")));
        assertThat(finance.debit(new DebitRequest("mysql-pay-1", 9001L, 101L, new BigDecimal("60.00")))
                .transactionId()).isEqualTo(debit.transactionId());
        var refund = finance.refund(new RefundRequest(
                "mysql-refund-1", "mysql-pay-1", 9001L, 101L, new BigDecimal("20.00")));
        assertThat(finance.refund(new RefundRequest(
                "mysql-refund-1", "mysql-pay-1", 9001L, 101L, new BigDecimal("20.00"))).transactionId())
                .isEqualTo(refund.transactionId());
        finance.settlement(new SettlementRequest(9001L, 7L, new BigDecimal("40.00")));
        finance.settlement(new SettlementRequest(9001L, 7L, new BigDecimal("40.00")));

        assertThat(personal(101)).isEqualByComparingTo("60.00");
        assertThat(business(7)).isEqualByComparingTo("40.00");
        assertThat(db.queryForObject("select count(*) from transaction_record", Integer.class)).isEqualTo(3);
        assertThat(db.queryForObject("select count(*) from outbox_event", Integer.class)).isEqualTo(3);
        BigDecimal delta = db.queryForObject(
                "select sum(amount) from transaction_record where user_id=101", BigDecimal.class);
        assertThat(new BigDecimal("100.00").add(delta)).isEqualByComparingTo(personal(101));

        assertThatThrownBy(() -> finance.refund(new RefundRequest(
                "mysql-refund-too-large", "mysql-pay-1", 9001L, 101L, new BigDecimal("41.00"))))
                .hasMessageContaining("累计退款金额");
        assertThatThrownBy(() -> finance.debit(new DebitRequest(
                "mysql-pay-no-money", 9002L, 101L, new BigDecimal("61.00"))))
                .hasMessageContaining("余额不足");
        assertThat(personal(101)).isEqualByComparingTo("60.00");
        assertThat(db.queryForObject("select count(*) from payment_request", Integer.class)).isEqualTo(3);
        assertThat(db.queryForObject("select count(*) from transaction_record", Integer.class)).isEqualTo(3);
        assertThat(db.queryForObject("select count(*) from outbox_event", Integer.class)).isEqualTo(3);
    }

    @Test
    void mysqlConcurrentSettlementCreditsSellerExactlyOnce() throws Exception {
        var pool = Executors.newFixedThreadPool(6);
        try {
            var startTogether = new CyclicBarrier(6);
            List<Callable<String>> calls = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                calls.add(() -> {
                    startTogether.await();
                    return finance.settlement(
                            new SettlementRequest(9900L, 7L, new BigDecimal("35.00"))).transactionId();
                });
            }
            var results = pool.invokeAll(calls).stream().map(future -> {
                try { return future.get(); } catch (Exception error) { throw new RuntimeException(error); }
            }).toList();
            assertThat(results).containsOnly(results.get(0));
            assertThat(business(7)).isEqualByComparingTo("35.00");
            assertThat(db.queryForObject(
                    "select count(*) from transaction_record where trade_type='SETTLEMENT'", Integer.class)).isOne();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void mysqlConcurrentRefundsAreSerializedAndCannotExceedOriginalDebit() throws Exception {
        finance.debit(new DebitRequest("mysql-pay-refund-race", 9100L, 101L, new BigDecimal("60.00")));
        var pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<String>> calls = List.of(
                    () -> refundOutcome("mysql-refund-race-a"),
                    () -> refundOutcome("mysql-refund-race-b"));
            List<String> outcomes = pool.invokeAll(calls).stream().map(future -> {
                try { return future.get(); } catch (Exception error) { throw new RuntimeException(error); }
            }).toList();
            assertThat(outcomes).containsExactlyInAnyOrder("COMPLETED", "REFUND_EXCEEDS_PAYMENT");
            assertThat(personal(101)).isEqualByComparingTo("80.00");
            assertThat(db.queryForObject(
                    "select count(*) from transaction_record where trade_type='REFUND'", Integer.class)).isOne();
            assertThat(db.queryForObject(
                    "select count(*) from outbox_event where event_type='RefundCompleted.v1'", Integer.class)).isOne();
        } finally {
            pool.shutdownNow();
        }
    }

    private String refundOutcome(String requestId) {
        try {
            finance.refund(new RefundRequest(
                    requestId, "mysql-pay-refund-race", 9100L, 101L, new BigDecimal("40.00")));
            return "COMPLETED";
        } catch (DomainException conflict) {
            return conflict.code;
        }
    }

    private BigDecimal personal(long userId) {
        return db.queryForObject("select personal_balance from balance where user_id=?", BigDecimal.class, userId);
    }

    private BigDecimal business(long userId) {
        return db.queryForObject("select business_balance from balance where user_id=?", BigDecimal.class, userId);
    }
}

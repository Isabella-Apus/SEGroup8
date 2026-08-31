package com.segroup8.finance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes=BenefitsFinanceApplication.class)
@AutoConfigureMockMvc
class InternalApiContractTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;

    @BeforeEach
    void reset() {
        db.update("delete from outbox_event");
        db.update("delete from transaction_record");
        db.update("delete from payment_request");
        db.update("delete from checkout_quote");
        db.update("delete from user_voucher");
        db.update("delete from voucher");
        db.update("delete from balance");
        db.update("insert into balance(user_id,personal_balance,business_balance,version) values(101,100,0,0)");
    }

    @Test
    void debitQueryRefundAndSettlementAreServiceAuthenticatedAndIdempotent() throws Exception {
        String debit = "{\"paymentRequestId\":\"contract-pay-1\",\"orderId\":7001,\"userId\":101,\"amount\":60}";
        mvc.perform(post("/internal/payments/debit").contentType(MediaType.APPLICATION_JSON).content(debit))
                .andExpect(status().isForbidden());
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/internal/payments/debit").header("X-Internal-Service-Token", "test-internal-token")
                            .contentType(MediaType.APPLICATION_JSON).content(debit))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.amount").value(60.0));
        }
        mvc.perform(get("/internal/payments/contract-pay-1").header("X-Internal-Service-Token", "test-internal-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requestType").value("DEBIT"));
        String refund = "{\"refundRequestId\":\"contract-refund-1\",\"paymentRequestId\":\"contract-pay-1\","
                + "\"orderId\":7001,\"userId\":101,\"amount\":20}";
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/internal/payments/refund").header("X-Internal-Service-Token", "test-internal-token")
                            .contentType(MediaType.APPLICATION_JSON).content(refund))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.requestType").value("REFUND"));
        }
        String settlement = "{\"orderId\":7001,\"sellerId\":7,\"amount\":40}";
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/internal/settlements").header("X-Internal-Service-Token", "test-internal-token")
                            .contentType(MediaType.APPLICATION_JSON).content(settlement))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.requestType").value("SETTLEMENT"));
        }
        org.assertj.core.api.Assertions.assertThat(
                db.queryForObject("select count(*) from transaction_record", Integer.class)).isEqualTo(3);
    }

    @Test
    void reserveAndReleaseCompensateUsingSameOrderRequestId() throws Exception {
        db.update("insert into voucher(id,issuer_type,voucher_type,issuer_user_id,scope_type,can_stack,name,discount_type,"
                + "discount_amount,min_amount,total_count,received_count,used_count,start_time,end_time,status,version) "
                + "values(80,'SELLER','SELLER',7,'SHOP',false,'contract','AMOUNT',10,0,1,1,0,"
                + "timestamp '2026-01-01 00:00:00',timestamp '2030-01-01 00:00:00','ACTIVE',0)");
        db.update("insert into user_voucher(user_id,voucher_id,status,expires_at,version) values(101,80,'AVAILABLE',"
                + "timestamp '2030-01-01 00:00:00',0)");
        String action = "{\"orderRequestId\":\"contract-order-1\",\"userId\":101,\"voucherId\":80,\"orderId\":7001}";
        mvc.perform(post("/internal/vouchers/reserve").header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(action))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESERVED"));
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/internal/vouchers/release").header("X-Internal-Service-Token", "test-internal-token")
                            .contentType(MediaType.APPLICATION_JSON).content(action))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AVAILABLE"));
        }
    }

    @Test
    void internalIdempotencyKeysRejectDifferentPaymentQuoteAndVoucherParameters() throws Exception {
        String debit = "{\"paymentRequestId\":\"reuse-pay\",\"orderId\":7101,\"userId\":101,\"amount\":10}";
        mvc.perform(post("/internal/payments/debit").header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(debit))
                .andExpect(status().isOk());
        mvc.perform(post("/internal/payments/debit").header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(debit.replace("\"amount\":10", "\"amount\":11")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));

        String quote = "{\"orderRequestId\":\"reuse-quote\",\"userId\":101,\"amount\":50}";
        mvc.perform(post("/internal/checkout/quote").header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(quote))
                .andExpect(status().isOk());
        mvc.perform(post("/internal/checkout/quote").header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(quote.replace("\"amount\":50", "\"amount\":51")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));

        db.update("insert into voucher(id,issuer_type,voucher_type,issuer_user_id,scope_type,can_stack,name,discount_type,"
                + "discount_amount,min_amount,total_count,received_count,used_count,start_time,end_time,status,version) "
                + "values(81,'SELLER','SELLER',7,'SHOP',false,'one','AMOUNT',10,0,1,1,0,"
                + "timestamp '2026-01-01 00:00:00',timestamp '2030-01-01 00:00:00','ACTIVE',0),"
                + "(82,'SELLER','SELLER',7,'SHOP',false,'two','AMOUNT',10,0,1,1,0,"
                + "timestamp '2026-01-01 00:00:00',timestamp '2030-01-01 00:00:00','ACTIVE',0)");
        db.update("insert into user_voucher(user_id,voucher_id,status,expires_at,version) values"
                + "(101,81,'AVAILABLE',timestamp '2030-01-01 00:00:00',0),"
                + "(101,82,'AVAILABLE',timestamp '2030-01-01 00:00:00',0)");
        String reserveOne = "{\"orderRequestId\":\"reuse-reserve\",\"userId\":101,\"voucherId\":81,\"orderId\":7101}";
        mvc.perform(post("/internal/vouchers/reserve").header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(reserveOne))
                .andExpect(status().isOk());
        mvc.perform(post("/internal/vouchers/reserve").header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(reserveOne.replace("81", "82")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }
}

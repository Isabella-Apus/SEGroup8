package com.segroup8.finance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes=BenefitsFinanceApplication.class)
@AutoConfigureMockMvc
class PublicApiTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired ObjectMapper json;

    @BeforeEach
    void reset() {
        db.update("delete from outbox_event");
        db.update("delete from transaction_record");
        db.update("delete from payment_request");
        db.update("delete from checkout_quote");
        db.update("delete from user_voucher");
        db.update("delete from voucher");
        db.update("delete from balance");
    }

    @Test
    void uc21AndUc22VoucherLifecycleQuoteReserveConsume() throws Exception {
        String voucherJson = """
                {"name":"满100减20","discountType":"AMOUNT","discountAmount":20,"minAmount":100,
                 "totalCount":2,"startTime":"2026-01-01T00:00:00Z","endTime":"2030-01-01T00:00:00Z",
                 "shopId":88,"scopeType":"SHOP"}
                """;
        String created = mvc.perform(post("/api/voucher/seller").header("Authorization", TestJwt.bearer(7, "OFFICIAL_SELLER"))
                        .contentType(MediaType.APPLICATION_JSON).content(voucherJson))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        long voucherId = json.readTree(created).get("id").asLong();

        mvc.perform(get("/api/voucher/list").header("Authorization", TestJwt.bearer(101, "USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(voucherId));
        mvc.perform(post("/api/voucher/{id}/claim", voucherId).header("Authorization", TestJwt.bearer(101, "USER")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("AVAILABLE"));
        mvc.perform(get("/api/voucher/my").header("Authorization", TestJwt.bearer(101, "USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(voucherId));
        mvc.perform(post("/api/voucher/{id}/claim", voucherId).header("Authorization", TestJwt.bearer(101, "USER")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VOUCHER_ALREADY_CLAIMED"));

        String quote = "{\"orderRequestId\":\"order-req-1\",\"userId\":101,\"amount\":120,"
                + "\"voucherId\":" + voucherId + ",\"shopIds\":[88],\"productIds\":[]}";
        mvc.perform(post("/internal/checkout/quote").header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(quote))
                .andExpect(status().isOk()).andExpect(jsonPath("$.discountAmount").value(20.0))
                .andExpect(jsonPath("$.payableAmount").value(100.0));
        String action = "{\"orderRequestId\":\"order-req-1\",\"userId\":101,\"voucherId\":" + voucherId
                + ",\"orderId\":9001}";
        mvc.perform(post("/internal/vouchers/reserve").header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(action))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESERVED"));
        mvc.perform(post("/internal/vouchers/consume").header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(action))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("USED"));
        mvc.perform(post("/internal/vouchers/consume").header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(action))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("USED"));
    }

    @Test
    void apiEnforcesJwtRolesValidationAndServiceIdentity() throws Exception {
        mvc.perform(get("/api/finance/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/voucher/admin/list").header("Authorization", TestJwt.bearer(101, "USER")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ROLE_FORBIDDEN"));
        mvc.perform(post("/api/finance/recharge").header("Authorization", TestJwt.bearer(101, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"bad\",\"amount\":0,\"channel\":\"WECHAT\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mvc.perform(get("/internal/payments/unknown").header("Authorization", TestJwt.bearer(101, "USER")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("SERVICE_IDENTITY_FORBIDDEN"));
    }

    @Test
    void authenticatedPublicRouteReturnsStableNotFoundError() throws Exception {
        mvc.perform(post("/api/voucher/seller/{id}/close", 999_999)
                        .header("Authorization", TestJwt.bearer(7, "OFFICIAL_SELLER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VOUCHER_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void uc23RechargeIsIdempotentAndRecordsArePrivate() throws Exception {
        String body = "{\"requestId\":\"recharge-1\",\"amount\":88.50,\"channel\":\"WECHAT\"}";
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/finance/recharge").header("Authorization", TestJwt.bearer(101, "USER"))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        }
        mvc.perform(get("/api/finance/dashboard").header("Authorization", TestJwt.bearer(101, "USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.personalBalance").value(88.50));
        mvc.perform(get("/api/finance/my-wallet/records").header("Authorization", TestJwt.bearer(101, "USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/finance/business/records").header("Authorization", TestJwt.bearer(101, "USER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/internal/settlements").header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":7301,\"sellerId\":7,\"amount\":25}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/finance/business/records")
                        .header("Authorization", TestJwt.bearer(7, "OFFICIAL_SELLER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].tradeType").value("SETTLEMENT"));
    }

    @Test
    void uc21SellerAndAdminCrudEnforceOwnershipAndState() throws Exception {
        String sellerBody = voucherBody("seller-crud", 88);
        long sellerVoucher = idOf(mvc.perform(post("/api/voucher/seller")
                        .header("Authorization", TestJwt.bearer(7, "OFFICIAL_SELLER"))
                        .contentType(MediaType.APPLICATION_JSON).content(sellerBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        mvc.perform(get("/api/voucher/seller/list").header("Authorization", TestJwt.bearer(7, "OFFICIAL_SELLER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(sellerVoucher));
        mvc.perform(put("/api/voucher/seller/{id}", sellerVoucher)
                        .header("Authorization", TestJwt.bearer(8, "OFFICIAL_SELLER"))
                        .contentType(MediaType.APPLICATION_JSON).content(voucherBody("stolen", 99)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("VOUCHER_NOT_OWNED"));
        mvc.perform(put("/api/voucher/seller/{id}", sellerVoucher)
                        .header("Authorization", TestJwt.bearer(7, "OFFICIAL_SELLER"))
                        .contentType(MediaType.APPLICATION_JSON).content(voucherBody("seller-updated", 88)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("seller-updated"));
        mvc.perform(post("/api/voucher/seller/{id}/close", sellerVoucher)
                        .header("Authorization", TestJwt.bearer(7, "OFFICIAL_SELLER")))
                .andExpect(status().isNoContent());

        long deletable = idOf(mvc.perform(post("/api/voucher/seller")
                        .header("Authorization", TestJwt.bearer(7, "OFFICIAL_SELLER"))
                        .contentType(MediaType.APPLICATION_JSON).content(voucherBody("seller-delete", 88)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        mvc.perform(delete("/api/voucher/seller/{id}", deletable)
                        .header("Authorization", TestJwt.bearer(7, "OFFICIAL_SELLER")))
                .andExpect(status().isNoContent());

        String adminBody = """
                {"name":"platform","discountType":"RATE","discountRate":0.8,"minAmount":10,"totalCount":5,
                 "startTime":"2026-01-01T00:00:00Z","endTime":"2030-01-01T00:00:00Z","scopeType":"PLATFORM"}
                """;
        long adminVoucher = idOf(mvc.perform(post("/api/voucher/admin")
                        .header("Authorization", TestJwt.bearer(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(adminBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        mvc.perform(get("/api/voucher/admin/list").header("Authorization", TestJwt.bearer(1, "ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(adminVoucher));
        mvc.perform(put("/api/voucher/admin/{id}", adminVoucher).header("Authorization", TestJwt.bearer(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(adminBody.replace("platform", "platform-updated")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("platform-updated"));
        mvc.perform(post("/api/voucher/admin/{id}/close", adminVoucher)
                        .header("Authorization", TestJwt.bearer(1, "ADMIN")))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/voucher/admin/{id}", adminVoucher).header("Authorization", TestJwt.bearer(1, "ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void everyPublicOperationRequiresBearerJwt() throws Exception {
        RequestBuilder[] operations = {
                get("/api/voucher/seller/list"),
                post("/api/voucher/seller").contentType(MediaType.APPLICATION_JSON).content("{}"),
                put("/api/voucher/seller/1").contentType(MediaType.APPLICATION_JSON).content("{}"),
                post("/api/voucher/seller/1/close"), delete("/api/voucher/seller/1"),
                get("/api/voucher/admin/list"),
                post("/api/voucher/admin").contentType(MediaType.APPLICATION_JSON).content("{}"),
                put("/api/voucher/admin/1").contentType(MediaType.APPLICATION_JSON).content("{}"),
                post("/api/voucher/admin/1/close"), delete("/api/voucher/admin/1"),
                get("/api/voucher/list"), post("/api/voucher/1/claim"), get("/api/voucher/my"),
                get("/api/finance/dashboard"),
                post("/api/finance/recharge").contentType(MediaType.APPLICATION_JSON).content("{}"),
                get("/api/finance/my-wallet/records"), get("/api/finance/business/records")
        };
        for (RequestBuilder operation : operations) {
            mvc.perform(operation).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
        }
    }

    @Test
    void privilegedVoucherAndBusinessRoutesRejectOrdinaryUsers() throws Exception {
        String user = TestJwt.bearer(101, "USER");
        String valid = voucherBody("role-check", 88);
        RequestBuilder[] operations = {
                get("/api/voucher/seller/list").header("Authorization", user),
                post("/api/voucher/seller").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON).content(valid),
                put("/api/voucher/seller/1").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON).content(valid),
                post("/api/voucher/seller/1/close").header("Authorization", user),
                delete("/api/voucher/seller/1").header("Authorization", user),
                get("/api/voucher/admin/list").header("Authorization", user),
                post("/api/voucher/admin").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON).content(valid),
                put("/api/voucher/admin/1").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON).content(valid),
                post("/api/voucher/admin/1/close").header("Authorization", user),
                delete("/api/voucher/admin/1").header("Authorization", user),
                get("/api/finance/business/records").header("Authorization", user)
        };
        for (RequestBuilder operation : operations) {
            mvc.perform(operation).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ROLE_FORBIDDEN"));
        }
    }

    @Test
    void malformedPayloadInvalidVoucherStateAndIdempotencyReuseReturnStableErrors() throws Exception {
        String seller = TestJwt.bearer(7, "OFFICIAL_SELLER");
        mvc.perform(post("/api/voucher/seller").header("Authorization", seller)
                        .contentType(MediaType.APPLICATION_JSON).content("{not-json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mvc.perform(post("/api/voucher/seller").header("Authorization", seller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voucherBody("invalid", 88).replace("\"totalCount\":10", "\"totalCount\":0")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mvc.perform(post("/api/voucher/seller").header("Authorization", seller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voucherBody("invalid-time", 88)
                                .replace("2026-01-01T00:00:00Z", "2031-01-01T00:00:00Z")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VOUCHER_TIME_INVALID"));
        mvc.perform(post("/api/voucher/seller").header("Authorization", seller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"invalid-rate","discountType":"RATE","discountRate":1.2,
                                 "minAmount":10,"totalCount":5,"startTime":"2026-01-01T00:00:00Z",
                                 "endTime":"2030-01-01T00:00:00Z","shopId":88,"scopeType":"SHOP"}
                                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VOUCHER_DISCOUNT_INVALID"));

        long claimed = idOf(mvc.perform(post("/api/voucher/seller").header("Authorization", seller)
                        .contentType(MediaType.APPLICATION_JSON).content(voucherBody("claimed", 88)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/voucher/{id}/claim", claimed)
                        .header("Authorization", TestJwt.bearer(101, "USER")))
                .andExpect(status().isCreated());
        mvc.perform(delete("/api/voucher/seller/{id}", claimed).header("Authorization", seller))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VOUCHER_ALREADY_CLAIMED"));
        mvc.perform(post("/api/voucher/seller/{id}/close", claimed).header("Authorization", seller))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/voucher/seller/{id}", claimed).header("Authorization", seller)
                        .contentType(MediaType.APPLICATION_JSON).content(voucherBody("closed-edit", 88)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VOUCHER_CLOSED"));

        String recharge = "{\"requestId\":\"same-recharge\",\"amount\":10,\"channel\":\"WECHAT\"}";
        mvc.perform(post("/api/finance/recharge").header("Authorization", TestJwt.bearer(101, "USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(recharge))
                .andExpect(status().isOk());
        mvc.perform(post("/api/finance/recharge").header("Authorization", TestJwt.bearer(101, "USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(recharge.replace("10", "11")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    private String voucherBody(String name, long shopId) {
        return "{\"name\":\"" + name + "\",\"discountType\":\"AMOUNT\",\"discountAmount\":10,"
                + "\"minAmount\":0,\"totalCount\":10,\"startTime\":\"2026-01-01T00:00:00Z\","
                + "\"endTime\":\"2030-01-01T00:00:00Z\",\"shopId\":" + shopId + ",\"scopeType\":\"SHOP\"}";
    }

    private long idOf(String content) throws Exception { return json.readTree(content).get("id").asLong(); }
}

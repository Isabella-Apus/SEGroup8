package com.segroup8.platform.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("DOMAIN_E")
@Tag("UC22")
class VoucherCheckoutUc22IntegrationTest extends DomainEIntegrationTestBase {

    @Test
    void claimedVoucherIsUsedOnceByPaidOrder() throws Exception {
        long voucherId = createVoucher(
                "/api/voucher/seller", sellerToken, "UC22结算优惠券", "10.00");

        mvc.perform(post("/api/voucher/{id}/claim", voucherId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(get("/api/voucher/my/available/reasons")
                        .param("shopIds", "100")
                        .param("totalAmount", "50.00")
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value(
                        org.hamcrest.Matchers.containsString("门槛不足")));

        mvc.perform(get("/api/voucher/my/available")
                        .param("shopIds", "100")
                        .param("totalAmount", "198.00")
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(voucherId));

        MvcResult orderCreated = mvc.perform(post("/api/order/create")
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1001,\"quantity\":2}],"
                                + "\"voucherId\":" + voucherId + "}"))
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
                .andExpect(jsonPath("$.code").value(0));

        assertEquals(2, db.queryForObject(
                "select status from user_voucher where user_id = 1 and voucher_id = ?",
                Integer.class, voucherId));
        assertEquals(orderId, db.queryForObject(
                "select used_order_id from user_voucher where user_id = 1 and voucher_id = ?",
                Long.class, voucherId));
        assertEquals(new BigDecimal("188.00"), db.queryForObject(
                "select payable_amount from order_info where id = ?", BigDecimal.class, orderId));
    }

    @Test
    void failedCheckoutDoesNotOccupyVoucherAndCanceledOrderReleasesIt() throws Exception {
        long voucherId = createVoucher(
                "/api/voucher/seller", sellerToken, "UC22失败释放券", "10.00");
        mvc.perform(post("/api/voucher/{id}/claim", voucherId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(post("/api/order/create")
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1001,\"quantity\":1}],"
                                + "\"voucherId\":" + voucherId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        assertEquals(0, db.queryForObject(
                "select count(*) from user_voucher where voucher_id = ? and used_order_id is not null",
                Integer.class, voucherId));

        MvcResult created = mvc.perform(post("/api/order/create")
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1001,\"quantity\":2}],"
                                + "\"voucherId\":" + voucherId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long orderId = responseData(created).path("id").asLong();

        mvc.perform(post("/api/order/{id}/cancel", orderId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertEquals(0, db.queryForObject(
                "select count(*) from user_voucher where voucher_id = ? and used_order_id is not null",
                Integer.class, voucherId));
    }
}

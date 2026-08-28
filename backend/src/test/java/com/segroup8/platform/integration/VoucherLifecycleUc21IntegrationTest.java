package com.segroup8.platform.integration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.segroup8.platform.entity.Voucher;
import com.segroup8.platform.mapper.VoucherMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("DOMAIN_E")
@Tag("UC21")
class VoucherLifecycleUc21IntegrationTest extends DomainEIntegrationTestBase {

    @Autowired
    private VoucherMapper voucherMapper;

    @Test
    void sellerAndAdminManageOwnedVoucherLifecycle() throws Exception {
        long sellerVoucherId = createVoucher(
                "/api/voucher/seller", sellerToken, "UC21卖家优惠券", "10.00");

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

        long adminVoucherId = createVoucher(
                "/api/voucher/admin", adminToken, "UC21平台优惠券", "8.00");
        mvc.perform(get("/api/voucher/admin/list")
                        .param("name", "UC21平台优惠券")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(adminVoucherId))
                .andExpect(jsonPath("$.data.records[0].shopId").doesNotExist());
    }

    @Test
    void otherSellerCannotManageVoucherAndUsedVoucherCannotBeDeleted() throws Exception {
        db.update("insert into user(id, username, password, nickname, role, status, create_time, update_time) "
                + "values(5, 'seller2', 'x', 'seller2', 'OFFICIAL_SELLER', 'ACTIVE', "
                + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        db.update("insert into shop(id, owner_user_id, name, status, create_time, update_time) "
                + "values(105, 5, 'other shop', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        String otherSellerToken = jwtUtils.createToken(5L, "seller2", "OFFICIAL_SELLER");
        long voucherId = createVoucher(
                "/api/voucher/seller", sellerToken, "UC21归属券", "10.00");

        mvc.perform(put("/api/voucher/seller/{id}", voucherId)
                        .header("Authorization", bearer(otherSellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voucherBody("越权修改", "8.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        Voucher used = new Voucher();
        used.setId(voucherId);
        used.setUsedCount(1);
        voucherMapper.updateById(used);
        mvc.perform(delete("/api/voucher/seller/{id}", voucherId)
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        assertEquals(1, db.queryForObject(
                "select count(*) from voucher where id = ?", Integer.class, voucherId));
    }

    @Test
    void invalidRulesAreRejectedWithoutWritingVoucher() throws Exception {
        ObjectNode invalidType = voucherJson("UC21非法类型");
        invalidType.put("type", 9);
        assertInvalid(invalidType);

        ObjectNode negativeDiscount = voucherJson("UC21负优惠");
        negativeDiscount.put("discountAmount", -1);
        assertInvalid(negativeDiscount);

        ObjectNode invalidThreshold = voucherJson("UC21非法门槛");
        invalidThreshold.put("minAmount", 0);
        assertInvalid(invalidThreshold);

        ObjectNode invalidTime = voucherJson("UC21非法时间");
        invalidTime.set("endTime", invalidTime.get("startTime"));
        invalidTime.set("startTime", invalidTime.get("grabEndTime"));
        assertInvalid(invalidTime);

        ObjectNode invalidTotal = voucherJson("UC21非法数量");
        invalidTotal.put("totalCount", 0);
        assertInvalid(invalidTotal);

        assertEquals(0, db.queryForObject("select count(*) from voucher", Integer.class));
    }

    @Test
    void closedVoucherCannotBeClaimed() throws Exception {
        long voucherId = createVoucher(
                "/api/voucher/seller", sellerToken, "UC21关闭不可领取", "10.00");
        mvc.perform(post("/api/voucher/seller/{id}/close", voucherId)
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(post("/api/voucher/{id}/claim", voucherId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        assertEquals(0, db.queryForObject(
                "select count(*) from user_voucher where voucher_id = ?", Integer.class, voucherId));
    }

    private ObjectNode voucherJson(String name) throws Exception {
        return (ObjectNode) objectMapper.readTree(voucherBody(name, "10.00"));
    }

    private void assertInvalid(ObjectNode body) throws Exception {
        mvc.perform(post("/api/voucher/seller")
                        .header("Authorization", bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}

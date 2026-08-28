package com.segroup8.platform.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("DOMAIN_E")
@Tag("UC22")
class VoucherClaimUc22IntegrationTest extends DomainEIntegrationTestBase {

    @Test
    void claimIsPersistedAndDuplicateClaimIsRejected() throws Exception {
        long voucherId = createVoucher(
                "/api/voucher/seller", sellerToken, "UC22领取优惠券", "10.00");

        mvc.perform(post("/api/voucher/{id}/claim", voucherId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(post("/api/voucher/{id}/claim", voucherId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void closedNotStartedEndedAndSoldOutVouchersDoNotCreateUserVoucher() throws Exception {
        long closed = createVoucher(
                "/api/voucher/seller", sellerToken, "UC22关闭券", "10.00");
        mvc.perform(post("/api/voucher/seller/{id}/close", closed)
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertClaimRejected(closed);

        ObjectNode notStartedBody = voucherJson("UC22未开始券");
        notStartedBody.put("grabStartTime", LocalDateTime.now().plusDays(1).toString());
        notStartedBody.put("grabEndTime", LocalDateTime.now().plusDays(2).toString());
        notStartedBody.put("startTime", LocalDateTime.now().plusDays(1).toString());
        long notStarted = createVoucher(notStartedBody);
        assertClaimRejected(notStarted);

        ObjectNode endedBody = voucherJson("UC22已结束券");
        endedBody.put("grabStartTime", LocalDateTime.now().minusDays(3).toString());
        endedBody.put("grabEndTime", LocalDateTime.now().minusDays(2).toString());
        endedBody.put("startTime", LocalDateTime.now().minusDays(3).toString());
        long ended = createVoucher(endedBody);
        assertClaimRejected(ended);

        ObjectNode soldOutBody = voucherJson("UC22领完券");
        soldOutBody.put("totalCount", 1);
        long soldOut = createVoucher(soldOutBody);
        String anotherBuyerToken = jwtUtils.createToken(4L, "buyer2", "USER");
        mvc.perform(post("/api/voucher/{id}/claim", soldOut)
                        .header("Authorization", bearer(anotherBuyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertClaimRejected(soldOut);

        org.junit.jupiter.api.Assertions.assertEquals(1, db.queryForObject(
                "select count(*) from user_voucher", Integer.class));
    }

    private void assertClaimRejected(long voucherId) throws Exception {
        mvc.perform(post("/api/voucher/{id}/claim", voucherId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    private ObjectNode voucherJson(String name) throws Exception {
        return (ObjectNode) objectMapper.readTree(voucherBody(name, "10.00"));
    }

    private long createVoucher(ObjectNode body) throws Exception {
        MvcResult result = mvc.perform(post("/api/voucher/seller")
                        .header("Authorization", bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return responseData(result).path("id").asLong();
    }
}

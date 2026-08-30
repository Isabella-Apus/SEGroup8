package com.segroup8.secondhand.api;

import static com.segroup8.secondhand.support.TestJwt.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.segroup8.secondhand.support.SecondhandIntegrationSupport;
import java.math.BigDecimal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@Tag("DOMAIN_D")
class SecondhandTradeApiTest extends SecondhandIntegrationSupport {
    @Autowired ObjectMapper json;

    @Test
    @Tag("UC17")
    void directBuyRequiresAuthenticationRejectsSelfPurchaseAndIsIdempotent() throws Exception {
        long productId = seedApprovedProduct(10, "二手键盘", "88.00", false);
        BuyRequest command = new BuyRequest(100L, "请包装好");

        mvc.perform(post("/api/secondhand/{id}/buy", productId)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(command)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));

        mvc.perform(post("/api/secondhand/{id}/buy", productId)
                        .header("Authorization", bearer(10, "seller"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(command)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("不能购买自己发布的二手商品"));

        String first = mvc.perform(post("/api/secondhand/{id}/buy", productId)
                        .header("Authorization", bearer(20, "buyer"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tradeType").value("DIRECT_BUY"))
                .andExpect(jsonPath("$.data.requestStatus").value("CREATED"))
                .andReturn().getResponse().getContentAsString();

        String repeated = mvc.perform(post("/api/secondhand/{id}/buy", productId)
                        .header("Authorization", bearer(20, "buyer"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestStatus").value("CREATED"))
                .andReturn().getResponse().getContentAsString();

        String repeatedBusinessKey = JsonPath.read(repeated, "$.data.orderBusinessKey");
        String firstBusinessKey = JsonPath.read(first, "$.data.orderBusinessKey");
        org.assertj.core.api.Assertions.assertThat(repeatedBusinessKey).isEqualTo(firstBusinessKey);
    }

    @Test
    @Tag("UC18")
    void bargainApiCoversApplyQueryOwnershipConfirmAndIllegalRepeat() throws Exception {
        long productId = seedApprovedProduct(10, "可议价教材", "80.00", true);
        BargainApplyRequest apply = new BargainApplyRequest(productId, 10L, new BigDecimal("65.00"));

        String applied = mvc.perform(post("/api/secondhand/trade/bargain/apply")
                        .header("Authorization", bearer(20, "buyer"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(apply)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        long negotiationId = ((Number) JsonPath.read(applied, "$.data.id")).longValue();

        mvc.perform(get("/api/secondhand/trade/bargain/list")
                        .header("Authorization", bearer(20, "buyer")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/secondhand/trade/bargain/effective")
                        .header("Authorization", bearer(20, "buyer")).param("productId", String.valueOf(productId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());

        BargainConfirmRequest confirm = new BargainConfirmRequest(negotiationId, new BigDecimal("68.00"), true);
        mvc.perform(post("/api/secondhand/trade/bargain/confirm")
                .header("Authorization", bearer(11, "other-seller"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(confirm)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("只有商品卖家可以确认议价"));

        mvc.perform(post("/api/secondhand/trade/bargain/confirm")
                .header("Authorization", bearer(10, "seller"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(confirm)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.orderRequestStatus").value("CREATED"));
        mvc.perform(get("/api/secondhand/trade/bargain/effective")
                        .header("Authorization", bearer(20, "buyer")).param("productId", String.valueOf(productId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
        mvc.perform(get("/api/secondhand/trade/bargain/list")
                        .header("Authorization", bearer(20, "buyer")).param("status", "ACCEPTED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(negotiationId));

        mvc.perform(post("/api/secondhand/trade/bargain/{id}/reject", negotiationId)
                        .header("Authorization", bearer(10, "seller")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("议价已处理，不能重复拒绝"));
    }

    @Test
    @Tag("UC19")
    void auctionApiCoversPublicReadBidRulesOwnershipSettlementAndFlow() throws Exception {
        long productId = seedApprovedProduct(10, "拍卖相机", "200.00", false);
        AuctionCreateRequest create = new AuctionCreateRequest(productId, new BigDecimal("100.00"),
                new BigDecimal("10.00"), 60);
        String created = mvc.perform(post("/api/secondhand/trade/auction")
                        .header("Authorization", bearer(10, "seller"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(create)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ONGOING"))
                .andReturn().getResponse().getContentAsString();
        long auctionId = ((Number) JsonPath.read(created, "$.data.id")).longValue();

        mvc.perform(get("/api/secondhand/trade/auction/product/{id}", productId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(auctionId));
        mvc.perform(get("/api/secondhand/trade/auction/seller/list")
                        .header("Authorization", bearer(10, "seller")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));

        mvc.perform(post("/api/secondhand/trade/auction/{id}/bid", auctionId)
                        .header("Authorization", bearer(10, "seller"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bidAmount\":100}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("卖家不能参与自己商品的竞拍"));
        mvc.perform(post("/api/secondhand/trade/auction/{id}/bid", auctionId)
                        .header("Authorization", bearer(20, "bidder"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bidAmount\":99}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("出价不得低于 100.00"));
        mvc.perform(post("/api/secondhand/trade/auction/{id}/bid", auctionId)
                        .header("Authorization", bearer(20, "bidder"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bidAmount\":100}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.currentBidderUserId").value(20));

        mvc.perform(post("/api/secondhand/trade/auction/{id}/close", auctionId)
                        .header("Authorization", bearer(11, "other-seller")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("只能结束自己发起的拍卖"));
        mvc.perform(post("/api/secondhand/trade/auction/{id}/close", auctionId)
                        .header("Authorization", bearer(10, "seller")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("FINISHED"))
                .andExpect(jsonPath("$.data.orderRequestStatus").value("CREATED"));

        long noBidProductId = seedApprovedProduct(10, "流拍商品", "100.00", false);
        AuctionCreateRequest noBid = new AuctionCreateRequest(noBidProductId, new BigDecimal("50.00"),
                new BigDecimal("5.00"), 60);
        String noBidJson = mvc.perform(post("/api/secondhand/trade/auction")
                        .header("Authorization", bearer(10, "seller"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(noBid)))
                .andReturn().getResponse().getContentAsString();
        long noBidAuctionId = ((Number) JsonPath.read(noBidJson, "$.data.id")).longValue();
        mvc.perform(post("/api/secondhand/trade/auction/{id}/flow", noBidAuctionId)
                        .header("Authorization", bearer(10, "seller")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("FLOW"));
    }

    @Test
    @Tag("UC20")
    void orderStatusEventRequiresServiceTokenAndConsumesEachEventOnce() throws Exception {
        long productId = seedApprovedProduct(10, "履约联动商品", "66.00", false);
        String purchased = mvc.perform(post("/api/secondhand/{id}/buy", productId)
                        .header("Authorization", bearer(20, "buyer"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"addressId\":100}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String businessKey = JsonPath.read(purchased, "$.data.orderBusinessKey");
        long orderId = ((Number) JsonPath.read(purchased, "$.data.orderId")).longValue();
        String event = json.writeValueAsString(java.util.Map.of(
                "eventId", "order-cancelled-1", "orderBusinessKey", businessKey,
                "orderId", orderId, "newStatus", "CANCELLED"));

        mvc.perform(post("/internal/events/order-status-changed")
                        .contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/events/order-status-changed")
                        .header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CONSUMED"));
        mvc.perform(post("/internal/events/order-status-changed")
                        .header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DUPLICATE"));

        org.assertj.core.api.Assertions.assertThat(db.queryForObject(
                "select status from secondhand_product where id=?", Integer.class, productId)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(db.queryForObject(
                "select count(*) from outbox_event where event_type='SecondhandOrderStatusObserved.v1'",
                Integer.class)).isEqualTo(1);
    }
}

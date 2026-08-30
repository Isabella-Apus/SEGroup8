package com.segroup8.order;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.order.DownstreamGateway.ProductSnapshot;
import com.segroup8.order.DownstreamGateway.Quote;
import com.segroup8.order.DownstreamGateway.RemoteResult;
import com.segroup8.order.DownstreamGateway.Reservation;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("DOMAIN_C")
class OrderApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @MockBean DownstreamGateway downstream;

    @BeforeEach void clean() {
        for (String table : List.of("outbox_event","order_saga","idempotency_record","logistics_trace","review",
                "order_after_sale_log","order_item","order_info")) db.update("delete from " + table);
        when(downstream.reserve(anyString(),anyLong(),anyList())).thenAnswer(inv -> new Reservation(inv.getArgument(0),
                List.of(new ProductSnapshot(10,"Snapshot phone",new BigDecimal("100.00"),1,2,20L))));
        when(downstream.quote(anyString(),anyLong(),any(),any())).thenReturn(new Quote(new BigDecimal("90.00"),
                new BigDecimal("10.00"),new BigDecimal("5.00"),new BigDecimal("5.00")));
        when(downstream.debit(anyString(),anyLong(),any(),any(),any())).thenReturn(RemoteResult.SUCCEEDED);
        when(downstream.paymentResult(anyString())).thenReturn(RemoteResult.SUCCEEDED);
        when(downstream.refund(anyString(),anyLong(),anyLong(),any())).thenReturn(RemoteResult.SUCCEEDED);
        when(downstream.refundResult(anyString())).thenReturn(RemoteResult.SUCCEEDED);
        when(downstream.settle(anyString(),anyLong(),anyLong(),any())).thenReturn(RemoteResult.SUCCEEDED);
        when(downstream.settlementResult(anyString())).thenReturn(RemoteResult.UNKNOWN);
    }

    @Test void actuatorExposesOperationsWithoutFlywayMetadata() throws Exception {
        mvc.perform(get("/actuator")).andExpect(status().isOk())
                .andExpect(jsonPath("$._links.health").exists())
                .andExpect(jsonPath("$._links.flyway").doesNotExist());
        mvc.perform(get("/actuator/info")).andExpect(status().isOk());
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_")));
        mvc.perform(get("/actuator/flyway")).andExpect(status().isNotFound());
    }

    @Test void publicApisCoverCreatePayFulfilReviewAndQueries() throws Exception {
        mvc.perform(post("/api/order/create").header("Idempotency-Key","create-unauth").contentType("application/json").content(createBody()))
                .andExpect(status().isUnauthorized());
        String created=mvc.perform(post("/api/order/create").headers(user(1)).header("Idempotency-Key","create-1")
                .contentType("application/json").content(createBody())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value(0)).andExpect(jsonPath("$.data.orderStatusKey").value("PENDING_PAY"))
                .andExpect(jsonPath("$.data.receiverPhoneMasked").value("138****8000"))
                .andReturn().getResponse().getContentAsString();
        long id=json.readTree(created).path("data").path("id").asLong();
        mvc.perform(post("/api/order/create").headers(user(1)).header("Idempotency-Key","create-1")
                .contentType("application/json").content(createBody())).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(id));
        mvc.perform(get("/api/order/list").headers(user(1))).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/order/detail/{id}",id).headers(user(9))).andExpect(status().isForbidden());
        mvc.perform(post("/api/order/{id}/pay",id).headers(user(1)).header("Idempotency-Key","pay-1").contentType("application/json").content("{\"payMode\":\"COIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(1));
        mvc.perform(get("/api/order/seller/list").headers(user(2))).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/order/seller/detail/{id}",id).headers(user(2))).andExpect(status().isOk());
        mvc.perform(post("/api/order/{id}/remind-ship",id).headers(user(1)).header("Idempotency-Key","remind-1")).andExpect(status().isOk()).andExpect(jsonPath("$.data").value("QUEUED"));
        mvc.perform(post("/api/order/{id}/ship",id).headers(user(2)).header("Idempotency-Key","ship-1").contentType("application/json").content("{\"deliveryNo\":\"SF123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(2));
        mvc.perform(post("/api/order/{id}/ship",id).headers(user(2)).header("Idempotency-Key","ship-1").contentType("application/json").content("{\"deliveryNo\":\"SF123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.deliveryNo").value("SF123"));
        mvc.perform(get("/api/logistics/order/{id}/trace",id).headers(user(1))).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].nodeName").value("已发货"));
        mvc.perform(post("/api/order/{id}/confirm-receive",id).headers(user(1)).header("Idempotency-Key","receive-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(3));
        mvc.perform(post("/api/order/{id}/confirm-receive",id).headers(user(1)).header("Idempotency-Key","receive-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(3));
        verify(downstream,times(1)).settle(anyString(),eq(id),eq(2L),eq(new BigDecimal("100.00")));
        mvc.perform(post("/api/order/{id}/review/items",id).headers(user(1)).header("Idempotency-Key","review-items-1").contentType("application/json")
                .content("{\"items\":[{\"productType\":\"NEW\",\"productId\":10,\"score\":5,\"content\":\"great\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(4));
        mvc.perform(post("/api/order/{id}/review/items",id).headers(user(1)).header("Idempotency-Key","review-items-1").contentType("application/json")
                .content("{\"items\":[{\"productType\":\"NEW\",\"productId\":10,\"score\":5,\"content\":\"great\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(4));
        mvc.perform(get("/api/review/my").headers(user(1))).andExpect(status().isOk()).andExpect(jsonPath("$.data.records[0].score").value(5));
        mvc.perform(post("/api/review/followup").headers(user(1)).header("Idempotency-Key","followup-1").contentType("application/json")
                .content("{\"orderId\":"+id+",\"productType\":\"NEW\",\"productId\":10,\"score\":5,\"content\":\"still great\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.reviewType").value("FOLLOWUP"));
        mvc.perform(get("/api/review/seller/list").headers(user(2))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test void refundAdminAndInternalApisAreProtectedAndIdempotent() throws Exception {
        String first=mvc.perform(post("/internal/orders/secondhand").header("X-Internal-Service-Token","test-internal-token")
                .contentType("application/json").content(secondhandBody())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id=json.readTree(first).get("id").asLong();
        mvc.perform(post("/internal/orders/secondhand").header("X-Internal-Service-Token","test-internal-token")
                .contentType("application/json").content(secondhandBody())).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/internal/orders/by-business-key/DIRECT:t-1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/orders/by-business-key/DIRECT:t-1").header("X-Internal-Service-Token","test-internal-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/internal/orders/{id}/snapshot",id).header("X-Internal-Service-Token","test-internal-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].productType").value("SECONDHAND"));
        db.update("update order_info set order_status='PENDING_SHIP',pay_status='PAID' where id=?",id);
        mvc.perform(post("/api/order/{id}/refund",id).headers(user(1)).header("Idempotency-Key","refund-request-1")
                .contentType("application/json").content("{\"reason\":\"damaged\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.refundStatus").value(1));
        mvc.perform(post("/api/admin/orders/{id}/refund/approve",id).headers(user(9)).header("Idempotency-Key","refund-decision-1"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/orders/{id}/refund/approve",id).headers(admin()).header("Idempotency-Key","refund-decision-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.refundStatus").value(2));
    }

    @Test void remainingPublicWriteEndpointsEnforceRolesAndState() throws Exception {
        long cancelId=createSecondhand("cancel");
        mvc.perform(post("/api/order/{id}/cancel",cancelId).headers(user(1)).header("Idempotency-Key","cancel-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(9));

        long completeId=createSecondhand("complete");
        db.update("update order_info set order_status='RECEIVED',pay_status='PAID' where id=?",completeId);
        mvc.perform(post("/api/order/{id}/complete",completeId).headers(user(1)).header("Idempotency-Key","complete-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(4));

        long reviewId=createSecondhand("single-review");
        db.update("update order_info set order_status='RECEIVED',pay_status='PAID' where id=?",reviewId);
        mvc.perform(post("/api/order/{id}/review",reviewId).headers(user(1)).header("Idempotency-Key","review-single-1").contentType("application/json")
                .content("{\"score\":4,\"content\":\"as described\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(4));
        long persistedReview=db.queryForObject("select id from review where order_id=?",Long.class,reviewId);
        mvc.perform(post("/api/review/{id}/reply",persistedReview).headers(user(9)).header("Idempotency-Key","reply-denied").contentType("application/json").content("{\"reply\":\"no\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/review/{id}/reply",persistedReview).headers(user(2)).header("Idempotency-Key","reply-ok").contentType("application/json").content("{\"reply\":\"thanks\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/review/seller/{id}/reply",persistedReview).headers(user(2)).header("Idempotency-Key","reply-ok").contentType("application/json").content("{\"reply\":\"thanks\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data").doesNotExist());
        mvc.perform(post("/api/logistics/trace").headers(user(2)).header("Idempotency-Key","trace-1").contentType("application/json")
                .content("{\"orderId\":"+reviewId+",\"nodeName\":\"hub\",\"statusDesc\":\"arrived\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nodeName").value("hub"));
        mvc.perform(post("/api/logistics/trace").headers(user(2)).header("Idempotency-Key","trace-1").contentType("application/json")
                .content("{\"orderId\":"+reviewId+",\"nodeName\":\"hub\",\"statusDesc\":\"arrived\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nodeName").value("hub"));
        mvc.perform(post("/api/logistics/push-next").headers(user(2)).header("Idempotency-Key","trace-next-1").contentType("application/json")
                .content("{\"orderId\":"+reviewId+"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nodeName").value("运输节点 2"));
        mvc.perform(get("/api/logistics/{id}",reviewId).headers(user(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));

        long rejectId=createSecondhand("reject");
        db.update("update order_info set order_status='PENDING_SHIP',pay_status='PAID' where id=?",rejectId);
        mvc.perform(post("/api/order/{id}/refund",rejectId).headers(user(1)).header("Idempotency-Key","request-reject").contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/order/{id}/refund/reject",rejectId).headers(user(2)).header("Idempotency-Key","seller-reject"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.refundStatus").value(3));
        mvc.perform(get("/api/admin/orders/{id}/after-sale-logs",rejectId).headers(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[2].action").value("REJECT"));

        long closeId=createSecondhand("close");
        mvc.perform(get("/api/admin/orders/list").headers(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").isNumber());
        mvc.perform(get("/api/admin/orders").headers(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").isNumber());
        mvc.perform(get("/api/admin/orders/detail/{id}",closeId).headers(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(closeId));
        mvc.perform(get("/api/admin/orders/{id}",closeId).headers(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(closeId));
        mvc.perform(post("/api/admin/orders/batch-close").headers(admin()).header("Idempotency-Key","batch-close-1").contentType("application/json")
                .content("{\"orderIds\":["+closeId+"]}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/order/detail/{id}",closeId).headers(user(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(9));

        long approveId=createSecondhand("seller-approve");
        db.update("update order_info set order_status='PENDING_SHIP',pay_status='PAID' where id=?",approveId);
        mvc.perform(post("/api/order/{id}/refund",approveId).headers(user(1)).header("Idempotency-Key","request-seller-approve").contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/order/{id}/refund/approve",approveId).headers(user(2)).header("Idempotency-Key","seller-approve"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.refundStatus").value(2));

        long adminRejectId=createSecondhand("admin-reject");
        db.update("update order_info set order_status='PENDING_SHIP',pay_status='PAID' where id=?",adminRejectId);
        mvc.perform(post("/api/order/{id}/refund",adminRejectId).headers(user(1)).header("Idempotency-Key","request-admin-reject").contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/admin/orders/{id}/refund/reject",adminRejectId).headers(admin()).header("Idempotency-Key","admin-reject"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.refundStatus").value(3));
    }

    @Test void unknownSettlementIsQueriedBeforeRetryAndNeverDoubleCreditsSeller() throws Exception {
        long id=createSecondhand("settlement-timeout");
        db.update("update order_info set order_status='SHIPPED',pay_status='PAID' where id=?",id);
        when(downstream.settle(anyString(),eq(id),eq(2L),any())).thenReturn(RemoteResult.UNKNOWN);
        when(downstream.settlementResult(anyString())).thenReturn(RemoteResult.UNKNOWN,RemoteResult.UNKNOWN,RemoteResult.SUCCEEDED);

        mvc.perform(post("/api/order/{id}/confirm-receive",id).headers(user(1)).header("Idempotency-Key","receive-timeout"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SETTLEMENT_TEMPORARILY_UNAVAILABLE"));
        mvc.perform(get("/api/order/detail/{id}",id).headers(user(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatusKey").value("SHIPPED"));
        mvc.perform(post("/api/order/{id}/confirm-receive",id).headers(user(1)).header("Idempotency-Key","receive-timeout"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(3));
        verify(downstream,times(1)).settle(anyString(),eq(id),eq(2L),eq(new BigDecimal("50.00")));
    }

    private org.springframework.http.HttpHeaders user(long id){var h=new org.springframework.http.HttpHeaders();h.set("X-User-Id",String.valueOf(id));h.set("X-User-Role","USER");return h;}
    private org.springframework.http.HttpHeaders admin(){var h=user(99);h.set("X-User-Role","ADMIN");return h;}
    private String createBody(){return "{\"items\":[{\"productId\":10,\"quantity\":1}],\"receiverName\":\"Buyer\",\"receiverPhone\":\"13800008000\",\"receiverProvince\":\"Zhejiang\",\"receiverCity\":\"Hangzhou\",\"receiverDetailAddress\":\"masked at rest boundary\",\"voucherId\":3}";}
    private long createSecondhand(String tradeId) throws Exception {String response=mvc.perform(post("/internal/orders/secondhand").header("X-Internal-Service-Token","test-internal-token").contentType("application/json").content(secondhandBody(tradeId))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return json.readTree(response).get("id").asLong();}
    private String secondhandBody(){return secondhandBody("t-1");}
    private String secondhandBody(String tradeId){return "{\"tradeType\":\"DIRECT\",\"tradeId\":\""+tradeId+"\",\"buyerUserId\":1,\"sellerUserId\":2,\"productId\":88,\"productName\":\"Used book\",\"price\":50,\"receiverName\":\"Buyer\",\"receiverPhone\":\"13800008000\",\"receiverProvince\":\"Zhejiang\",\"receiverCity\":\"Hangzhou\",\"receiverDetailAddress\":\"detail\"}";}
}

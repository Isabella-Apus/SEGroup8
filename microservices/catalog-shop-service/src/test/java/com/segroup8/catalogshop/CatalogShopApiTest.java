package com.segroup8.catalogshop;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc @Tag("DOMAIN_B")
class CatalogShopApiTest {
    @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired InventoryModule inventory;@Autowired OutboxPublisher outbox;
    @BeforeEach void seed(){
        for(String table:new String[]{"outbox_event","idempotency_record","inventory_reservation_item","inventory_reservation","product_risk_audit","browse_history","user_search_history","search_keyword_stat","product","shop","category"})db.update("delete from "+table);
        db.update("insert into category(id,parent_id,name,sort_order,active) values(1,null,'图书',1,true)");
        db.update("insert into shop(id,seller_id,merchant_application_id,name,status,decoration_template,decoration_json) values(10,7,'app-1','测试店','OPEN','CLASSIC','{}')");
        db.update("insert into product(id,seller_id,shop_id,category_id,name,description,price,stock,reserved_stock,status) values(100,7,10,1,'Java 图书','微服务',59,10,0,'ON_SALE')");
    }
    @Test void concurrentReservationsDoNotOversell() throws Exception {
        db.update("update product set stock=5 where id=100");
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var start=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Boolean> first=()->{start.await();try{inventory.reserve("race-1",new InventoryController.ReserveRequest("order-a",java.util.List.of(new InventoryController.Item(100,4))));return true;}catch(ApiException e){return false;}};
        java.util.concurrent.Callable<Boolean> second=()->{start.await();try{inventory.reserve("race-2",new InventoryController.ReserveRequest("order-b",java.util.List.of(new InventoryController.Item(100,4))));return true;}catch(ApiException e){return false;}};
        var a=pool.submit(first);var b=pool.submit(second);start.countDown();int successes=(a.get()?1:0)+(b.get()?1:0);pool.shutdownNow();
        org.junit.jupiter.api.Assertions.assertEquals(1,successes);org.junit.jupiter.api.Assertions.assertEquals(4,db.queryForObject("select reserved_stock from product where id=100",Integer.class));
    }
    @Test @Tag("UC06") void catalogEndpoints() throws Exception {
        mvc.perform(get("/api/category/tree")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("图书"));
        mvc.perform(get("/api/product/search").param("keyword","java").param("minPrice","50")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(100));
        mvc.perform(get("/api/product/list").param("shopId","10")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(100));
        mvc.perform(get("/api/product/detail/100")).andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(10));
        mvc.perform(get("/internal/products/100/snapshot").header("X-Internal-Service-Token","bad")).andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/products/100/snapshot").header("X-Internal-Service-Token","test-token")).andExpect(status().isOk()).andExpect(jsonPath("$.productId").value(100));
    }
    @Test void actuatorContract() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/info")).andExpect(status().isOk());
    }
    @Test @Tag("UC07") void sellerLifecycleAndOwnership() throws Exception {
        String body="{\"shopId\":10,\"categoryId\":1,\"name\":\"新书\",\"description\":\"正常商品\",\"price\":20,\"stock\":3}";
        mvc.perform(post("/api/product/seller").header("X-Seller-Id",7).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        mvc.perform(get("/api/product/seller/list").header("X-Seller-Id",7)).andExpect(status().isOk()).andExpect(jsonPath("$[0].sellerId").value(7));
        mvc.perform(get("/api/product/seller/100").header("X-Seller-Id",7)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(100));
        mvc.perform(put("/api/product/seller/100").header("X-Seller-Id",8).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNotFound());
        mvc.perform(put("/api/product/seller/100").header("X-Seller-Id",7).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        mvc.perform(delete("/api/product/seller/100").header("X-Seller-Id",7)).andExpect(status().isNoContent());
    }
    @Test @Tag("UC08") void shopAndMerchantEventAreIdempotent() throws Exception {
        mvc.perform(get("/api/shop/public/10")).andExpect(status().isOk()).andExpect(jsonPath("$.shop.name").value("测试店"));
        mvc.perform(get("/api/shop/seller/current").header("X-Seller-Id",7)).andExpect(status().isOk());
        mvc.perform(put("/api/shop/seller/current/settings").header("X-Seller-Id",7).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"新店名\",\"announcement\":\"欢迎\",\"open\":true}")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("新店名"));
        mvc.perform(put("/api/shop/seller/current/decoration").header("X-Seller-Id",7).contentType(MediaType.APPLICATION_JSON).content("{\"template\":\"GRID\",\"contentJson\":\"{}\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.decorationTemplate").value("GRID"));
        String event="{\"eventId\":\"evt-2\",\"applicationId\":\"app-2\",\"sellerId\":8,\"shopName\":\"第二店\"}";
        for(int i=0;i<2;i++)mvc.perform(post("/internal/events/merchant-approved").header("X-Internal-Service-Token","test-token").contentType(MediaType.APPLICATION_JSON).content(event)).andExpect(status().isAccepted()).andExpect(jsonPath("$.sellerId").value(8));
    }
    @Test @Tag("UC09") void deterministicRiskFallbackAndDecision() throws Exception {
        String body="{\"shopId\":10,\"categoryId\":1,\"name\":\"违禁商品\",\"description\":\"枪支\",\"price\":20,\"stock\":3}";
        mvc.perform(post("/api/product/seller").header("X-Seller-Id",7).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        long audit=db.queryForObject("select max(id) from product_risk_audit",Long.class);
        mvc.perform(get("/api/admin/product-risk-audits/"+audit).header("X-Admin-Id",1)).andExpect(status().isOk()).andExpect(jsonPath("$.riskLevel").value("HIGH")).andExpect(jsonPath("$.decisionReason").value("DETERMINISTIC_RULES_NO_LLM_KEY"));
        mvc.perform(post("/api/admin/product-risk-audits/"+audit+"/decision").header("X-Admin-Id",1).contentType(MediaType.APPLICATION_JSON).content("{\"approved\":false,\"reason\":\"规则命中\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        mvc.perform(get("/api/admin/product-risk-audits").header("X-Admin-Id",1).param("status","REJECTED")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(audit));
        org.junit.jupiter.api.Assertions.assertEquals(1,db.queryForObject("select count(*) from outbox_event where event_type='NotificationRequested.v1'",Integer.class));
        outbox.publishBatch();
        org.junit.jupiter.api.Assertions.assertEquals(1,db.queryForObject("select attempts from outbox_event where event_type='NotificationRequested.v1'",Integer.class));
    }
    @Test @Tag("UC10") void behaviorIsUserScopedAndCountsHotKeywords() throws Exception {
        mvc.perform(post("/api/user/browse-history").header("X-User-Id",2).contentType(MediaType.APPLICATION_JSON).content("{\"productId\":100,\"productType\":\"NEW\"}")).andExpect(status().isCreated());
        long browseId=db.queryForObject("select id from browse_history where user_id=2",Long.class);
        mvc.perform(get("/api/user/browse-history").header("X-User-Id",3)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        for(int i=0;i<2;i++)mvc.perform(post("/api/search/history").header("X-User-Id",2).contentType(MediaType.APPLICATION_JSON).content("{\"keyword\":\" Java  \"}")).andExpect(status().isCreated());
        mvc.perform(get("/api/search/hot")).andExpect(status().isOk()).andExpect(jsonPath("$[0].keyword").value("java")).andExpect(jsonPath("$[0].searchCount").value(2));
        mvc.perform(get("/api/search/history").header("X-User-Id",2)).andExpect(status().isOk()).andExpect(jsonPath("$[0].keyword").value("java"));
        mvc.perform(delete("/api/user/browse-history/"+browseId).header("X-User-Id",3)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/user/browse-history/"+browseId).header("X-User-Id",2)).andExpect(status().isNoContent());
        mvc.perform(post("/api/user/browse-history").header("X-User-Id",2).contentType(MediaType.APPLICATION_JSON).content("{\"productId\":100,\"productType\":\"NEW\"}")).andExpect(status().isCreated());
        mvc.perform(delete("/api/user/browse-history").header("X-User-Id",2)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/search/history").header("X-User-Id",2)).andExpect(status().isNoContent());
    }
    @Test void inventoryIsAtomicIdempotentAndStateful() throws Exception {
        String body="{\"orderId\":\"order-1\",\"items\":[{\"productId\":100,\"quantity\":3}]}";
        var first=mvc.perform(post("/internal/inventory/reservations").header("X-Internal-Service-Token","test-token").header("X-Idempotency-Key","idem-1").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("RESERVED")).andReturn();
        String id=com.jayway.jsonpath.JsonPath.read(first.getResponse().getContentAsString(),"$.id").toString();
        mvc.perform(get("/internal/inventory/reservations/"+id).header("X-Internal-Service-Token","test-token")).andExpect(status().isOk()).andExpect(jsonPath("$.orderId").value("order-1"));
        mvc.perform(post("/internal/inventory/reservations").header("X-Internal-Service-Token","test-token").header("X-Idempotency-Key","idem-1").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(Long.parseLong(id)));
        mvc.perform(post("/internal/inventory/reservations/"+id+"/confirm").header("X-Internal-Service-Token","test-token")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(post("/internal/inventory/reservations/"+id+"/confirm").header("X-Internal-Service-Token","test-token")).andExpect(status().isOk());
        mvc.perform(post("/internal/inventory/reservations/"+id+"/release").header("X-Internal-Service-Token","test-token")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE"));
        org.junit.jupiter.api.Assertions.assertEquals(7,db.queryForObject("select stock from product where id=100",Integer.class));
    }
}

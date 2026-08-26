package com.segroup8.shop;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ShopApiAndE2ETest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seed() {
        db.update("delete from shops");
        db.update("insert into shops(id,seller_id,name,announcement,status,decoration_template,decoration_json,updated_at) "
                + "values(8,7,'旧店名','','OPEN','CLASSIC','{}',CURRENT_TIMESTAMP)");
    }

    @Test
    void t0801_publicViewSettingsDecorationAndCatalogFallback() throws Exception {
        mvc.perform(get("/api/shops/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shop.name").value("旧店名"))
                .andExpect(jsonPath("$.products").isEmpty())
                .andExpect(jsonPath("$.catalogAvailable").value(false));
        mvc.perform(get("/api/shops/seller/current").header("X-Seller-Id", 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerId").value(7));
        mvc.perform(put("/api/shops/seller/current/settings")
                        .header("X-Seller-Id", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"第八组精品店\",\"announcement\":\"欢迎\",\"open\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("第八组精品店"))
                .andExpect(jsonPath("$.announcement").value("欢迎"));
        mvc.perform(put("/api/shops/seller/current/decoration")
                        .header("X-Seller-Id", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"template\":\"GRID\",\"contentJson\":\"{\\\"hero\\\":\\\"summer\\\"}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decorationTemplate").value("GRID"))
                .andExpect(jsonPath("$.decorationJson").value("{\"hero\":\"summer\"}"));
    }

    @Test
    void t0802_decorationRuleRejectsUnknownTemplate() {
        assertThatThrownBy(() -> DecorationPolicy.validate("SCRIPT", "{}"))
                .isInstanceOf(ShopException.class)
                .hasMessageContaining("装修模板");
    }

    @Test
    void t0803_closedShopIsNotPublicAndUnknownSellerCannotMaintainIt() throws Exception {
        mvc.perform(put("/api/shops/seller/current/settings")
                        .header("X-Seller-Id", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"暂停营业\",\"announcement\":\"维护中\",\"open\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        mvc.perform(get("/api/shops/8"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SHOP_NOT_FOUND"));
        mvc.perform(get("/api/shops/seller/current").header("X-Seller-Id", 99))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SHOP_NOT_FOUND"));
        mvc.perform(put("/api/shops/seller/current/settings")
                        .header("X-Seller-Id", 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"越权店铺\",\"announcement\":\"\",\"open\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SHOP_NOT_FOUND"));
    }

    @Test
    void t0804_rejectsMalformedNonObjectAndOversizedDecoration() throws Exception {
        assertInvalidDecoration("[]");
        assertInvalidDecoration("{bad-json}");
        String oversized = "x".repeat(20001);
        String body = objectMapper.writeValueAsString(
                Map.of("template", "GRID", "contentJson", "{\"content\":\"" + oversized + "\"}"));
        mvc.perform(put("/api/shops/seller/current/decoration")
                        .header("X-Seller-Id", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_DECORATION"));
    }

    private void assertInvalidDecoration(String contentJson) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("template", "GRID", "contentJson", contentJson));
        mvc.perform(put("/api/shops/seller/current/decoration")
                        .header("X-Seller-Id", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_DECORATION"));
    }
}

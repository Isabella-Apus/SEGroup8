package com.segroup8.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class CatalogLifecycleApiAndE2ETest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void resetData() {
        db.update("delete from integration_outbox");
        db.update("delete from products");
    }

    @Test
    void t0701_mainLifecycleAndSellerApis() throws Exception {
        long id = createDraft(7, "新商品");
        mvc.perform(get("/api/catalog/seller/products").header("X-Seller-Id", 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(put("/api/catalog/seller/products/{id}", id)
                        .header("X-Seller-Id", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("修改商品")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("修改商品"));
        submit(id, 7).andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        mvc.perform(post("/api/catalog/internal/products/{id}/risk-decision", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_SALE"));
        mvc.perform(get("/api/catalog/products/{id}", id)).andExpect(status().isOk());
        mvc.perform(post("/api/catalog/seller/products/{id}/actions/OFF_SHELF", id)
                        .header("X-Seller-Id", 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OFF_SHELF"));
    }

    @Test
    void t0702_rejectedProductCanBeEditedResubmittedAndArchived() throws Exception {
        long id = createDraft(7, "待驳回商品");
        submit(id, 7).andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        mvc.perform(post("/api/catalog/internal/products/{id}/risk-decision", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":false}"))
                .andExpect(jsonPath("$.status").value("REJECTED"));
        mvc.perform(put("/api/catalog/seller/products/{id}", id)
                        .header("X-Seller-Id", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("修改后商品")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("修改后商品"));
        submit(id, 7).andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

        long archived = createDraft(7, "待归档商品");
        mvc.perform(post("/api/catalog/seller/products/{id}/actions/ARCHIVE", archived)
                        .header("X-Seller-Id", 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void t0703_rejectsUnauthorizedAndIllegalOperationsAndPersistsOutbox() throws Exception {
        long id = createDraft(7, "权限商品");
        mvc.perform(put("/api/catalog/seller/products/{id}", id)
                        .header("X-Seller-Id", 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("越权修改")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
        mvc.perform(post("/api/catalog/seller/products/{id}/actions/OFF_SHELF", id)
                        .header("X-Seller-Id", 7))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_PRODUCT_TRANSITION"));
        submit(id, 7).andExpect(status().isOk());
        assertThat(db.queryForObject(
                "select count(*) from integration_outbox where aggregate_id=?", Integer.class, id))
                .isEqualTo(1);
    }

    @Test
    void t0704_validatesFieldsAndPreventsPendingReviewEdits() throws Exception {
        mvc.perform(post("/api/catalog/seller/products")
                        .header("X-Seller-Id", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shopId\":8,\"name\":\"无效商品\",\"category\":\"DIGITAL\",\"price\":0,\"stock\":-1}"))
                .andExpect(status().isBadRequest());
        long id = createDraft(7, "待审核商品");
        submit(id, 7).andExpect(status().isOk());
        mvc.perform(put("/api/catalog/seller/products/{id}", id)
                        .header("X-Seller-Id", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("审核中修改")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_PRODUCT_STATE"));
    }

    @Test
    void t0705_lifecycleRuleRejectsIllegalTransition() {
        assertThatThrownBy(() -> LifecyclePolicy.next("DRAFT", "OFF_SHELF"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("不能执行");
    }

    private long createDraft(long sellerId, String name) throws Exception {
        String created = mvc.perform(post("/api/catalog/seller/products")
                        .header("X-Seller-Id", sellerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(created).get("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions submit(long id, long sellerId) throws Exception {
        return mvc.perform(post("/api/catalog/seller/products/{id}/actions/SUBMIT", id)
                .header("X-Seller-Id", sellerId));
    }

    private String productBody(String name) {
        return "{\"shopId\":8,\"name\":\"" + name
                + "\",\"description\":\"安全商品\",\"category\":\"DIGITAL\",\"price\":99.00,\"stock\":3}";
    }
}

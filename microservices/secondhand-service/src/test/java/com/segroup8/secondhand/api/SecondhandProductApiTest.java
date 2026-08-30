package com.segroup8.secondhand.api;

import static com.segroup8.secondhand.support.TestJwt.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.segroup8.secondhand.support.SecondhandIntegrationSupport;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@Tag("DOMAIN_D")
@Tag("UC16")
class SecondhandProductApiTest extends SecondhandIntegrationSupport {
    @Autowired ObjectMapper json;

    @Test
    void publicReadAndProtectedWriteEnforceAuthenticationOwnershipAndRiskGate() throws Exception {
        mvc.perform(get("/api/secondhand/list"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));

        ProductSaveRequest command = validProduct("离散数学教材");
        mvc.perform(post("/api/secondhand/seller").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(command)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));

        String createdJson = mvc.perform(post("/api/secondhand/seller")
                        .header("Authorization", bearer(10, "alice"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(command)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.riskAudit.auditStatus").value("RISK_PENDING"))
                .andReturn().getResponse().getContentAsString();
        long productId = ((Number) JsonPath.read(createdJson, "$.data.id")).longValue();

        mvc.perform(get("/api/secondhand/list"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));

        mvc.perform(post("/internal/events/product-risk-decided")
                        .header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"risk-1\",\"productId\":" + productId + ",\"decision\":\"APPROVED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CONSUMED"));
        mvc.perform(post("/internal/events/product-risk-decided")
                        .header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"risk-1\",\"productId\":" + productId + ",\"decision\":\"APPROVED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DUPLICATE"));

        mvc.perform(get("/api/secondhand/list").param("keyword", "数学"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(productId));

        mvc.perform(put("/api/secondhand/seller/{id}", productId)
                        .header("Authorization", bearer(11, "mallory"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(command)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void invalidCategoryAndPriceAreRejected() throws Exception {
        ProductSaveRequest invalidCategory = new ProductSaveRequest("商品", null, List.of("/a.png"), null,
                new BigDecimal("100"), new BigDecimal("50"), 99, 9999, "九成新", 1, 1);
        mvc.perform(post("/api/secondhand/seller").header("Authorization", bearer(10, "alice"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(invalidCategory)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("所选二手分类不存在或已停用"));

        ProductSaveRequest invalidPrice = new ProductSaveRequest("商品", null, List.of("/a.png"), null,
                new BigDecimal("50"), new BigDecimal("100"), 8, 801, "九成新", 1, 1);
        mvc.perform(post("/api/secondhand/seller").header("Authorization", bearer(10, "alice"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(invalidPrice)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("二手售价不能高于原价"));
    }

    private ProductSaveRequest validProduct(String name) {
        return new ProductSaveRequest(name, null, List.of("/images/book.png"), "课程教材",
                new BigDecimal("120.00"), new BigDecimal("48.00"), 8, 801, "九五新", 1, 1);
    }
}

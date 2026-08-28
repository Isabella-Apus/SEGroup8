package com.segroup8.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("DOMAIN_D")
@Tag("UC16")
@Sql(scripts = "/integration/uc16-product-management-setup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SecondhandProductManagementIntegrationTest {

    private static final long SELLER_ID = 1601L;
    private static final long OTHER_USER_ID = 1602L;
    private static final int CATEGORY_ID = 1601;
    private static final int SUB_CATEGORY_ID = 1602;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String sellerToken;
    private String otherToken;

    @BeforeEach
    void createTokens() {
        sellerToken = jwtUtils.createToken(SELLER_ID, "uc16_seller", "USER");
        otherToken = jwtUtils.createToken(OTHER_USER_ID, "uc16_other", "USER");
    }

    @Test
    void realCategoryCreateEditShelfAndReload_arePersistedAcrossHttpAndDatabase() throws Exception {
        mockMvc.perform(get("/api/category/tree")
                        .param("scene", "SECONDHAND")
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.id == 1601)].name").value("UC16 Digital"))
                .andExpect(jsonPath("$.data[?(@.id == 1601)].children[0].id").value(SUB_CATEGORY_ID));

        ObjectNode createPayload = validPayload("UC16 Created Product");
        MvcResult createResult = mockMvc.perform(post("/api/secondhand/seller")
                        .header("Authorization", bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("UC16 Created Product"))
                .andExpect(jsonPath("$.data.images.length()").value(2))
                .andReturn();

        long productId = responseData(createResult).path("id").asLong();
        assertThat(productId).isPositive();
        assertThat(text("SELECT name FROM secondhand_product WHERE id = ?", productId))
                .isEqualTo("UC16 Created Product");
        assertThat(number("SELECT status FROM secondhand_product WHERE id = ?", productId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM product_risk_audit WHERE product_type = 'SECONDHAND' AND product_id = ?",
                productId)).isEqualTo(1);

        ObjectNode editPayload = validPayload("UC16 Persisted Edit");
        editPayload.put("description", "edited through the real HTTP service");
        editPayload.put("salePrice", "88.50");
        mockMvc.perform(put("/api/secondhand/seller/{productId}", productId)
                        .header("Authorization", bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editPayload.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("UC16 Persisted Edit"))
                .andExpect(jsonPath("$.data.salePrice").value(88.5));

        assertThat(text("SELECT description FROM secondhand_product WHERE id = ?", productId))
                .isEqualTo("edited through the real HTTP service");
        assertThat(money("SELECT sale_price FROM secondhand_product WHERE id = ?", productId))
                .isEqualByComparingTo("88.50");

        changeStatus(productId, 2, sellerToken, 0);
        assertThat(number("SELECT status FROM secondhand_product WHERE id = ?", productId)).isEqualTo(2);
        assertPublicListContains("UC16 Persisted Edit", productId, false);

        changeStatus(productId, 1, sellerToken, 0);
        assertThat(number("SELECT status FROM secondhand_product WHERE id = ?", productId)).isEqualTo(1);
        assertPublicListContains("UC16 Persisted Edit", productId, true);

        MvcResult sellerList = mockMvc.perform(get("/api/secondhand/seller/list")
                        .header("Authorization", bearer(sellerToken))
                        .param("keyword", "UC16 Persisted Edit")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        assertThat(recordIds(sellerList)).contains(productId);
    }

    @Test
    void nonOwnerCannotEditShelfOrDelete_andProductRemainsUnchanged() throws Exception {
        ObjectNode payload = validPayload("UC16 Unauthorized Edit");

        mockMvc.perform(put("/api/secondhand/seller/1601")
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
        changeStatus(1601L, 2, otherToken, 403);
        mockMvc.perform(delete("/api/secondhand/seller/1601")
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        assertThat(text("SELECT name FROM secondhand_product WHERE id = 1601"))
                .isEqualTo("UC16 Owned Product");
        assertThat(number("SELECT status FROM secondhand_product WHERE id = 1601")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM secondhand_product WHERE id = 1601")).isEqualTo(1);
    }

    @Test
    void soldProductCannotBeEditedRelistedOrDeleted() throws Exception {
        ObjectNode payload = validPayload("UC16 Sold Product Edited");
        payload.put("status", 1);

        mockMvc.perform(put("/api/secondhand/seller/1603")
                        .header("Authorization", bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("已售商品不能编辑、上架或删除"));
        changeStatus(1603L, 1, sellerToken, 400);
        mockMvc.perform(delete("/api/secondhand/seller/1603")
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        assertThat(number("SELECT status FROM secondhand_product WHERE id = 1603")).isEqualTo(3);
        assertThat(text("SELECT name FROM secondhand_product WHERE id = 1603"))
                .isEqualTo("UC16 Sold Product");
    }

    @Test
    void ownerDeleteRemovesProductFromSellerPublicAndDetailQueries() throws Exception {
        mockMvc.perform(delete("/api/secondhand/seller/1605")
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertThat(count("SELECT COUNT(*) FROM secondhand_product WHERE id = 1605")).isZero();
        mockMvc.perform(get("/api/secondhand/detail/1605")
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));

        MvcResult sellerList = mockMvc.perform(get("/api/secondhand/seller/list")
                        .header("Authorization", bearer(sellerToken))
                        .param("keyword", "UC16 Delete Product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        assertThat(recordIds(sellerList)).doesNotContain(1605L);
    }

    @Test
    void invalidNameImagesCategoryConditionNegotiableAndPrices_doNotWriteProducts() throws Exception {
        long before = count("SELECT COUNT(*) FROM secondhand_product WHERE seller_user_id = ?", SELLER_ID);
        List<ObjectNode> invalidPayloads = List.of(
                validPayload(" "),
                validPayload("UC16 No Images").set("images", objectMapper.createArrayNode()),
                withTenImages(validPayload("UC16 Too Many Images")),
                validPayload("UC16 No Condition").put("conditionLevel", " "),
                validPayload("UC16 No Origin").putNull("originPrice"),
                validPayload("UC16 Zero Sale").put("salePrice", 0),
                validPayload("UC16 Reversed Price").put("originPrice", 50).put("salePrice", 80),
                validPayload("UC16 Invalid Main").put("categoryId", 9999),
                validPayload("UC16 Invalid Child").put("subCategoryId", 9999),
                validPayload("UC16 Invalid Negotiable").put("isNegotiable", 2),
                validPayload("UC16 Price Overflow").put("salePrice", "123456789.01")
        );

        for (ObjectNode payload : invalidPayloads) {
            mockMvc.perform(post("/api/secondhand/seller")
                            .header("Authorization", bearer(sellerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }

        assertThat(count("SELECT COUNT(*) FROM secondhand_product WHERE seller_user_id = ?", SELLER_ID))
                .isEqualTo(before);
    }

    private ObjectNode validPayload(String name) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("name", name);
        ArrayNode images = payload.putArray("images");
        images.add("/uploads/uc16-primary.png");
        images.add("/uploads/uc16-detail.png");
        payload.put("originPrice", "160.00");
        payload.put("salePrice", "96.00");
        payload.put("categoryId", CATEGORY_ID);
        payload.put("subCategoryId", SUB_CATEGORY_ID);
        payload.put("conditionLevel", "90%");
        payload.put("isNegotiable", 1);
        payload.put("description", "UC16 real persistence fixture");
        return payload;
    }

    private ObjectNode withTenImages(ObjectNode payload) {
        ArrayNode images = payload.putArray("images");
        for (int i = 0; i < 10; i++) {
            images.add("/uploads/uc16-" + i + ".png");
        }
        return payload;
    }

    private void changeStatus(long productId, int targetStatus, String token, int expectedCode) throws Exception {
        mockMvc.perform(post("/api/secondhand/seller/{productId}/status", productId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":" + targetStatus + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    private void assertPublicListContains(String keyword, long productId, boolean expected) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/secondhand/list")
                        .header("Authorization", bearer(sellerToken))
                        .param("keyword", keyword)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        if (expected) {
            assertThat(recordIds(result)).contains(productId);
        } else {
            assertThat(recordIds(result)).doesNotContain(productId);
        }
    }

    private List<Long> recordIds(MvcResult result) throws Exception {
        JsonNode records = objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/records");
        if (!records.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(records.spliterator(), false)
                .map(node -> node.path("id").asLong())
                .toList();
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private int number(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Integer.class, args);
    }

    private String text(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, String.class, args);
    }

    private BigDecimal money(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
    }
}

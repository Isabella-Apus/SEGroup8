package com.segroup8.behavior;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("DOMAIN_B")
@Tag("UC10")
class BehaviorApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;

    @BeforeEach
    void clean() {
        db.update("delete from browse_history");
        db.update("delete from search_history");
        db.update("delete from keyword_stats");
    }

    @Test
    void browseSearchHotAndDeleteUseTheDatabase() throws Exception {
        browse(3, 1);
        String rows = mvc.perform(get("/api/behavior/browse-history").header("X-User-Id", 3))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].productId").value(1))
                .andReturn().getResponse().getContentAsString();
        long historyId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(rows).get(0).get("id").asLong();
        mvc.perform(delete("/api/behavior/browse-history/{id}", historyId).header("X-User-Id", 3))
                .andExpect(status().isNoContent());
        search(3, "  Java   Book ");
        mvc.perform(get("/api/behavior/search-history").header("X-User-Id", 3))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].keyword").value("java book"));
        mvc.perform(get("/api/behavior/hot-keywords")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].searchCount").value(1));
    }

    @Test
    void keywordRuleNormalizesAndRejectsBlank() {
        assertThat(KeywordPolicy.normalize(" A   B ")).isEqualTo("a b");
        assertThatThrownBy(() -> KeywordPolicy.normalize("  ")).isInstanceOf(BehaviorException.class);
    }

    @Test
    void browseHistoryDeduplicatesAndOrdersNewestFirst() throws Exception {
        browse(3, 1);
        browse(3, 2);
        browse(3, 1);
        assertThat(db.queryForObject(
                "select count(*) from browse_history where user_id=3 and product_id=1", Integer.class)).isEqualTo(1);
        db.update("update browse_history set browsed_at=TIMESTAMP '2026-01-01 00:00:00' where user_id=3 and product_id=2");
        db.update("update browse_history set browsed_at=TIMESTAMP '2026-01-02 00:00:00' where user_id=3 and product_id=1");
        mvc.perform(get("/api/behavior/browse-history").header("X-User-Id", 3))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].productId").value(1))
                .andExpect(jsonPath("$[1].productId").value(2));
    }

    @Test
    void historyDeletionIsUserScoped() throws Exception {
        browse(3, 1);
        long id = db.queryForObject("select id from browse_history where user_id=3", Long.class);
        mvc.perform(delete("/api/behavior/browse-history/{id}", id).header("X-User-Id", 4))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HISTORY_NOT_FOUND"));
        assertThat(db.queryForObject("select count(*) from browse_history where id=?", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void hotKeywordsAreRankedLimitedAndValidated() throws Exception {
        search(3, "Java"); search(3, "Spring"); search(4, "Spring");
        search(5, "Vue"); search(6, "Vue"); search(7, "Vue");
        mvc.perform(get("/api/behavior/hot-keywords")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].keyword").value("vue"))
                .andExpect(jsonPath("$[1].keyword").value("spring"));
        String tooLong = "a".repeat(65);
        String body = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                java.util.Map.of("keyword", tooLong));
        mvc.perform(post("/api/behavior/search-history").header("X-User-Id", 3)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_KEYWORD"));
    }

    private void browse(long userId, long productId) throws Exception {
        mvc.perform(post("/api/behavior/browse-history").header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"productType\":\"NEW\"}"))
                .andExpect(status().isCreated());
    }

    private void search(long userId, String keyword) throws Exception {
        String body = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("keyword", keyword));
        mvc.perform(post("/api/behavior/search-history").header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }
}

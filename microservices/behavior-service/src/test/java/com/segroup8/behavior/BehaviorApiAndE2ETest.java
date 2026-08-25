package com.segroup8.behavior;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc
class BehaviorApiAndE2ETest {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;
 @BeforeEach void clean(){db.update("delete from browse_history");db.update("delete from search_history");db.update("delete from keyword_stats");}
 @Test void uc10BrowseSearchHotE2EAndAllApis() throws Exception{
  mvc.perform(post("/api/behavior/browse-history").header("X-User-Id",3).contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1,\"productType\":\"NEW\"}")) .andExpect(status().isCreated());
  String rows=mvc.perform(get("/api/behavior/browse-history").header("X-User-Id",3)).andExpect(status().isOk()).andExpect(jsonPath("$[0].productId").value(1)).andReturn().getResponse().getContentAsString();
  long historyId=new com.fasterxml.jackson.databind.ObjectMapper().readTree(rows).get(0).get("id").asLong();
  mvc.perform(delete("/api/behavior/browse-history/{id}",historyId).header("X-User-Id",3)).andExpect(status().isNoContent());
  mvc.perform(post("/api/behavior/search-history").header("X-User-Id",3).contentType(MediaType.APPLICATION_JSON).content("{\"keyword\":\"  Java   Book \"}")) .andExpect(status().isCreated());
  mvc.perform(get("/api/behavior/search-history").header("X-User-Id",3)).andExpect(status().isOk()).andExpect(jsonPath("$[0].keyword").value("java book"));
  mvc.perform(get("/api/behavior/hot-keywords")).andExpect(status().isOk()).andExpect(jsonPath("$[0].searchCount").value(1));
  mvc.perform(delete("/api/behavior/browse-history").header("X-User-Id",3)).andExpect(status().isNoContent());
 }
 @Test void keywordRuleNormalizesAndRejectsBlank(){assertThat(KeywordPolicy.normalize(" A   B ")).isEqualTo("a b");assertThatThrownBy(()->KeywordPolicy.normalize("  ")).isInstanceOf(BehaviorException.class);}
}

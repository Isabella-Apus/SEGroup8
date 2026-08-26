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
 @Test void browseHistoryDeduplicatesOrdersAndIsolatesUsers() throws Exception{
  browse(3,1);browse(3,2);browse(3,1);browse(4,9);
  assertThat(db.queryForObject("select count(*) from browse_history where user_id=3 and product_id=1",Integer.class)).isEqualTo(1);
  db.update("update browse_history set browsed_at=TIMESTAMP '2026-01-01 00:00:00' where user_id=3 and product_id=2");
  db.update("update browse_history set browsed_at=TIMESTAMP '2026-01-02 00:00:00' where user_id=3 and product_id=1");
  mvc.perform(get("/api/behavior/browse-history").header("X-User-Id",3)).andExpect(status().isOk())
    .andExpect(jsonPath("$[0].productId").value(1)).andExpect(jsonPath("$[1].productId").value(2)).andExpect(jsonPath("$.length()").value(2));
 }
 @Test void browseDeleteAndClearAreUserScoped() throws Exception{
  browse(3,1);long id=db.queryForObject("select id from browse_history where user_id=3",Long.class);
  mvc.perform(delete("/api/behavior/browse-history/{id}",id).header("X-User-Id",4)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HISTORY_NOT_FOUND"));
  mvc.perform(delete("/api/behavior/browse-history").header("X-User-Id",4)).andExpect(status().isNoContent());
  assertThat(db.queryForObject("select count(*) from browse_history where user_id=3",Integer.class)).isEqualTo(1);
 }
 @Test void searchNormalizesRanksAndLimitsHotKeywords() throws Exception{
  search(3,"Java");search(3,"Spring");search(4,"Spring");search(5,"Vue");search(6,"Vue");search(7,"Vue");
  mvc.perform(get("/api/behavior/hot-keywords")).andExpect(status().isOk())
    .andExpect(jsonPath("$[0].keyword").value("vue")).andExpect(jsonPath("$[0].searchCount").value(3))
    .andExpect(jsonPath("$[1].keyword").value("spring")).andExpect(jsonPath("$[1].searchCount").value(2));
  db.update("delete from keyword_stats");for(int i=1;i<=11;i++)db.update("insert into keyword_stats(keyword,search_count,updated_at) values(?,?,CURRENT_TIMESTAMP)","word"+i,i);
  mvc.perform(get("/api/behavior/hot-keywords")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(10));
 }
 private void browse(long user,long product) throws Exception{mvc.perform(post("/api/behavior/browse-history").header("X-User-Id",user).contentType(MediaType.APPLICATION_JSON).content("{\"productId\":"+product+",\"productType\":\"NEW\"}")).andExpect(status().isCreated());}
 private void search(long user,String keyword) throws Exception{String body=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("keyword",keyword));mvc.perform(post("/api/behavior/search-history").header("X-User-Id",user).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());}
}

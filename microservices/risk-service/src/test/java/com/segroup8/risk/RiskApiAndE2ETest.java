package com.segroup8.risk;

import static org.assertj.core.api.Assertions.assertThat;
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
class RiskApiAndE2ETest {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;
 @BeforeEach void clean(){db.update("delete from integration_outbox");db.update("delete from risk_audits");}
 @Test void uc09SubmitRuleReviewDecisionE2EAndAllApis() throws Exception{
  String response=mvc.perform(post("/internal/risk-audits").contentType(MediaType.APPLICATION_JSON).content("{\"productId\":21,\"name\":\"违禁商品\",\"description\":\"测试\"}"))
    .andExpect(status().isCreated()).andExpect(jsonPath("$.riskLevel").value("HIGH")).andExpect(jsonPath("$.status").value("PENDING"))
    .andReturn().getResponse().getContentAsString();
  long id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asLong();
  mvc.perform(get("/api/admin/risk-audits").header("X-Admin-Id",99).param("status","PENDING"))
    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
  mvc.perform(post("/api/admin/risk-audits/{id}/decision",id).header("X-Admin-Id",99).contentType(MediaType.APPLICATION_JSON).content("{\"approved\":false,\"reason\":\"命中违禁词\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
  assertThat(db.queryForObject("select count(*) from integration_outbox",Integer.class)).isEqualTo(1);
 }
 @Test void keyRuleFindsForbiddenWords(){RuleResult result=RiskRule.evaluate("包含毒品描述");assertThat(result.level()).isEqualTo("HIGH");assertThat(result.hits()).contains("毒品");}
}

package com.segroup8.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc
class RiskApiAndE2ETest {
    @Autowired MockMvc mvc; @Autowired JdbcTemplate db; @Autowired ObjectMapper json;
    @BeforeEach void clean(){db.update("delete from integration_outbox");db.update("delete from risk_audits");}

    @Test void t0901HighRiskAuditCanBeListedAndRejected() throws Exception {
        long id=createAudit(21,"违禁商品","测试");
        mvc.perform(get("/api/admin/risk-audits").header("X-Admin-Id",99).param("status","PENDING"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(post("/api/admin/risk-audits/{id}/decision",id).header("X-Admin-Id",99)
            .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":false,\"reason\":\"命中违禁词\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED")).andExpect(jsonPath("$.adminId").value(99));
        assertThat(db.queryForObject("select count(*) from integration_outbox",Integer.class)).isEqualTo(1);
    }
    @Test void t0902ForbiddenWordRuleProducesHighRiskAndHits() throws Exception {
        mvc.perform(post("/internal/risk-audits").contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":22,\"name\":\"普通标题\",\"description\":\"包含毒品描述\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.riskLevel").value("HIGH")).andExpect(jsonPath("$.ruleHits").value("毒品"));
    }
    @Test void t0903LowRiskAuditCanBeApprovedAndCallbackFailureRecorded() throws Exception {
        long id=createAudit(23,"安全商品","常规描述");
        mvc.perform(post("/api/admin/risk-audits/{id}/decision",id).header("X-Admin-Id",100)
            .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true,\"reason\":\"人工复核通过\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.riskLevel").value("LOW")).andExpect(jsonPath("$.status").value("APPROVED"));
        assertThat(db.queryForObject("select status from integration_outbox",String.class)).isEqualTo("PENDING");
    }
    @Test void t0904RejectReasonAndSingleDecisionAreEnforced() throws Exception {
        long id=createAudit(24,"安全商品","常规描述");
        mvc.perform(post("/api/admin/risk-audits/{id}/decision",id).header("X-Admin-Id",99)
            .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":false,\"reason\":\"\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REASON_REQUIRED"));
        mvc.perform(post("/api/admin/risk-audits/{id}/decision",id).header("X-Admin-Id",99)
            .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}")) .andExpect(status().isOk());
        mvc.perform(post("/api/admin/risk-audits/{id}/decision",id).header("X-Admin-Id",99)
            .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}")) .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_DECIDED"));
    }
    private long createAudit(long productId,String name,String description) throws Exception {
        String body=mvc.perform(post("/internal/risk-audits").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(new RiskService.CreateCommand(productId,name,description))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }
}

package com.segroup8.platform.integration;

import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Tag("DOMAIN_A")
@Tag("UC05")
class ReportBlockCreditUc05IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @BeforeEach
    void resetGlobalState() {
        jdbcTemplate.update("delete from user_report");
        jdbcTemplate.update("delete from user_block");
        jdbcTemplate.update("delete from admin_audit_log");
        jdbcTemplate.update("delete from user where id != 2");
        jdbcTemplate.update("update user set username = 'admin1', password = 'x', nickname = '管理员1', role = 'ADMIN', status = 'NORMAL' where id = 2");
        jdbcTemplate.update("ALTER TABLE user ALTER COLUMN id RESTART WITH 3");
    }

    @Test
    void reportBlockCredit_shouldPersistOwnershipAuditAndIdempotency() throws Exception {
        String reporterUsername = "uc05-reporter-" + System.nanoTime();
        String targetUsername = "uc05-target-" + System.nanoTime();

        register(reporterUsername, "Reporter");
        register(targetUsername, "Target");

        long reporterId = userId(reporterUsername);
        long targetId = userId(targetUsername);
        String reporterToken = jwtUtils.createToken(reporterId, reporterUsername, "USER");
        String targetToken = jwtUtils.createToken(targetId, targetUsername, "USER");
        String adminToken = jwtUtils.createToken(2L, "admin1", "ADMIN");

        MvcResult reportResult = mockMvc.perform(post("/api/report-block/report")
                        .header("Authorization", bearer(reporterToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reportedId\":" + targetId
                                + ",\"reasonType\":\"FRAUD\""
                                + ",\"reasonDesc\":\"Invalid trade evidence\""
                                + ",\"tradeContext\":\"SH_SELLER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        assertTrue(reportResult.getResponse().getContentAsString().contains("\"code\":0"));

        mockMvc.perform(get("/api/report-block/report/my")
                        .header("Authorization", bearer(reporterToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].reportedId").value(targetId));

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", bearer(adminToken))
                        .param("page", "1")
                        .param("size", "10")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].reportedId").value(targetId));

        mockMvc.perform(post("/api/report-block/report")
                        .header("Authorization", bearer(reporterToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reportedId\":" + targetId + ",\"reasonType\":\"FRAUD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        long reportId = jdbcTemplate.queryForObject(
                "select max(id) from user_report where reporter_id = ? and reported_id = ?",
                Long.class, reporterId, targetId);

        mockMvc.perform(post("/api/report-block/block")
                        .header("Authorization", bearer(reporterToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"targetUserId\":" + targetId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/report-block/block/check/" + targetId)
                        .header("Authorization", bearer(reporterToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(get("/api/report-block/block/blocked-by/" + reporterId)
                        .header("Authorization", bearer(targetToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(get("/api/report-block/block/check/" + reporterId)
                        .header("Authorization", bearer(targetToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));

        mockMvc.perform(post("/api/report-block/block")
                        .header("Authorization", bearer(reporterToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"targetUserId\":" + targetId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(delete("/api/report-block/block/" + targetId)
                        .header("Authorization", bearer(reporterToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/report-block/block/check/" + targetId)
                        .header("Authorization", bearer(reporterToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
        mockMvc.perform(delete("/api/report-block/block/" + targetId)
                        .header("Authorization", bearer(reporterToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        Integer targetScoreBefore = jdbcTemplate.queryForObject(
                "select credit_score from user where id = ?", Integer.class, targetId);
        int creditLogsBefore = count("select count(*) from credit_score_log where user_id = ?", targetId);
        int auditLogsBefore = count("select count(*) from admin_audit_log where target_type = 'USER_REPORT' and target_id = ?",
                reportId);

        mockMvc.perform(post("/api/admin/reports/audit")
                        .header("Authorization", bearer(adminToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reportId\":" + reportId
                                + ",\"decision\":1,\"adminRemark\":\"confirmed\",\"customDelta\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertEquals(1, jdbcTemplate.queryForObject(
                "select status from user_report where id = ?", Integer.class, reportId));
        assertEquals(targetScoreBefore - 20, jdbcTemplate.queryForObject(
                "select credit_score from user where id = ?", Integer.class, targetId));
        assertEquals(creditLogsBefore + 1, count("select count(*) from credit_score_log where user_id = ?", targetId));
        assertEquals(-20, jdbcTemplate.queryForObject(
                "select delta from credit_score_log where user_id = ? and reason_code = 'REPORT_UPHELD' and ref_id = ?",
                Integer.class, targetId, reportId));
        assertEquals(auditLogsBefore + 1, count(
                "select count(*) from admin_audit_log where target_type = 'USER_REPORT' and target_id = ?", reportId));

        mockMvc.perform(post("/api/admin/reports/audit")
                        .header("Authorization", bearer(adminToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reportId\":" + reportId
                                + ",\"decision\":1,\"adminRemark\":\"duplicate\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        assertEquals(creditLogsBefore + 1, count("select count(*) from credit_score_log where user_id = ?", targetId));
        assertEquals(auditLogsBefore + 1, count(
                "select count(*) from admin_audit_log where target_type = 'USER_REPORT' and target_id = ?", reportId));

        assertEquals(1, count("select count(*) from user_report where reporter_id = ? and reported_id = ?",
                reporterId, targetId));

        mockMvc.perform(post("/api/report-block/report")
                        .header("Authorization", bearer(reporterToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reportedId\":" + reporterId + ",\"reasonType\":\"FRAUD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(post("/api/report-block/block")
                        .header("Authorization", bearer(reporterToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"targetUserId\":" + reporterId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", bearer(targetToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(post("/api/admin/reports/audit")
                        .header("Authorization", bearer(targetToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reportId\":" + reportId + ",\"decision\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    private void register(String username, String nickname) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + username
                                + "\",\"password\":\"User12345\",\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private long userId(String username) {
        return jdbcTemplate.queryForObject("select id from user where username = ?", Long.class, username);
    }

    private int count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Integer.class, args);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

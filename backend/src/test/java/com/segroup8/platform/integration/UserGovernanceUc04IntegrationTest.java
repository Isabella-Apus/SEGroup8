package com.segroup8.platform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("DOMAIN_A")
@Tag("UC04")
class UserGovernanceUc04IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    void banLoginFailureUnbanRecoveryPermissionAndAuditMustBeConsistent() throws Exception {
        String username = uniqueUsername();
        register(username);
        Long userId = jdbcTemplate.queryForObject("select id from user where username = ?", Long.class, username);
        String userToken = login(username, "secret123");
        String adminToken = jwtUtils.createToken(2L, "admin1", "ADMIN");

        int auditsBefore = jdbcTemplate.queryForObject(
                "select count(*) from admin_audit_log where target_type = 'USER' and target_id = ?",
                Integer.class, userId);
        mockMvc.perform(put("/api/admin/users/{id}/ban", userId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertThat(jdbcTemplate.queryForObject("select status from user where id = ?", String.class, userId))
                .isEqualTo("BANNED");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(put("/api/admin/users/{id}/unban", userId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertThat(jdbcTemplate.queryForObject("select status from user where id = ?", String.class, userId))
                .isEqualTo("NORMAL");
        String recoveredToken = login(username, "secret123");
        assertThat(recoveredToken).isNotBlank();

        mockMvc.perform(put("/api/admin/users/{id}/ban", userId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(put("/api/admin/users/{id}/ban", 2L)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(put("/api/admin/users/{id}/unban", userId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertThat(jdbcTemplate.queryForObject("select status from user where id = ?", String.class, userId))
                .isEqualTo("NORMAL");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from admin_audit_log where target_type = 'USER' and target_id = ?",
                Integer.class, userId)).isEqualTo(auditsBefore + 3);

        mockMvc.perform(get("/api/admin/audit-logs?targetType=USER")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").isNumber());
        mockMvc.perform(get("/api/admin/audit-logs?targetType=USER")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\",\"nickname\":\"Governance User\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("token").asText();
    }

    private String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private String uniqueUsername() {
        return "uc04-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

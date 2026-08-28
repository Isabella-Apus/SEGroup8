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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("DOMAIN_A")
@Tag("UC03")
class MerchantApplicationUc03IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    void submitApproveUpgradeShopNotificationAndAuditMustBeConsistent() throws Exception {
        String username = uniqueUsername("uc03-approved");
        register(username, "Applicant");
        String userToken = login(username, "secret123");
        String adminToken = jwtUtils.createToken(2L, "admin1", "ADMIN");

        submit(userToken, "Approved Shop");
        Long applicationId = latestApplicationId(username);
        mockMvc.perform(get("/api/user/merchant-application/me")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.storeName").value("Approved Shop"));
        mockMvc.perform(get("/api/admin/merchant-applications?status=0")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        int notificationsBefore = count("notification", "user_id", userId(username));
        int auditsBefore = count("admin_audit_log", "target_id", applicationId);
        mockMvc.perform(post("/api/admin/merchant-applications/{id}/approve", applicationId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertThat(jdbcTemplate.queryForObject(
                "select status from merchant_application where id = ?", Integer.class, applicationId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select role from user where username = ?", String.class, username)).isEqualTo("OFFICIAL_SELLER");
        assertThat(count("shop", "owner_user_id", userId(username))).isEqualTo(1);
        assertThat(count("notification", "user_id", userId(username))).isEqualTo(notificationsBefore + 1);
        assertThat(count("admin_audit_log", "target_id", applicationId)).isEqualTo(auditsBefore + 1);
        mockMvc.perform(get("/api/user/profile").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("OFFICIAL_SELLER"))
                .andExpect(jsonPath("$.data.shopName").value("Approved Shop"));

        int shopsAfterApproval = count("shop", "owner_user_id", userId(username));
        int notificationsAfterApproval = count("notification", "user_id", userId(username));
        mockMvc.perform(post("/api/admin/merchant-applications/{id}/approve", applicationId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertThat(count("shop", "owner_user_id", userId(username))).isEqualTo(shopsAfterApproval);
        assertThat(count("notification", "user_id", userId(username))).isEqualTo(notificationsAfterApproval);

        String rejectedUsername = uniqueUsername("uc03-rejected");
        register(rejectedUsername, "Rejected Applicant");
        String rejectedToken = login(rejectedUsername, "secret123");
        submit(rejectedToken, "Rejected Shop");
        Long rejectedId = latestApplicationId(rejectedUsername);
        mockMvc.perform(post("/api/admin/merchant-applications/{id}/reject", rejectedId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectReason\":\"missing license\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertThat(jdbcTemplate.queryForObject(
                "select status from merchant_application where id = ?", Integer.class, rejectedId)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "select reject_reason from merchant_application where id = ?", String.class, rejectedId))
                .isEqualTo("missing license");
        assertThat(jdbcTemplate.queryForObject(
                "select role from user where username = ?", String.class, rejectedUsername)).isEqualTo("USER");
    }

    private void submit(String token, String storeName) throws Exception {
        mockMvc.perform(post("/api/user/merchant-application")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson(storeName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void register(String username, String nickname) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\","
                                + "\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("token").asText();
    }

    private Long userId(String username) {
        return jdbcTemplate.queryForObject("select id from user where username = ?", Long.class, username);
    }

    private Long latestApplicationId(String username) {
        return jdbcTemplate.queryForObject(
                "select max(id) from merchant_application where user_id = (select id from user where username = ?)",
                Long.class, username);
    }

    private int count(String table, String column, Long value) {
        Integer result = jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where " + column + " = ?", Integer.class, value);
        return result == null ? 0 : result;
    }

    private String applicationJson(String storeName) {
        return "{\"storeName\":\"" + storeName + "\",\"categoryId\":1,\"idCardNo\":\"110101199001011234\","
                + "\"bankCardNo\":\"6222021234567890123\",\"licenseImg\":\"/license.png\","
                + "\"warehouseAddr\":\"Guangdong Shenzhen Warehouse\",\"warehouseProvince\":\"Guangdong\","
                + "\"warehouseCity\":\"Shenzhen\",\"warehouseDetail\":\"Nanshan Road\","
                + "\"contactName\":\"Applicant\",\"contactPhone\":\"13800138000\"}";
    }

    private String uniqueUsername(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

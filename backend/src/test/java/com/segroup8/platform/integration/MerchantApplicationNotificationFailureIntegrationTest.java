package com.segroup8.platform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.entity.Notification;
import com.segroup8.platform.mapper.NotificationMapper;
import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("DOMAIN_A")
@Tag("UC03")
class MerchantApplicationNotificationFailureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @SpyBean
    private NotificationMapper notificationMapper;

    @Test
    void notificationStorageFailureMustNotRollbackApprovalCoreState() throws Exception {
        String username = uniqueUsername();
        register(username);
        String userToken = login(username, "secret123");
        String adminToken = jwtUtils.createToken(2L, "admin1", "ADMIN");
        submit(userToken);
        Long applicationId = jdbcTemplate.queryForObject(
                "select max(id) from merchant_application where user_id = (select id from user where username = ?)",
                Long.class, username);

        doThrow(new RuntimeException("notification store unavailable"))
                .when(notificationMapper).insert(ArgumentMatchers.any(Notification.class));

        mockMvc.perform(post("/api/admin/merchant-applications/{id}/approve", applicationId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Long userId = jdbcTemplate.queryForObject("select id from user where username = ?", Long.class, username);
        assertThat(jdbcTemplate.queryForObject(
                "select status from merchant_application where id = ?", Integer.class, applicationId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select role from user where id = ?", String.class, userId)).isEqualTo("OFFICIAL_SELLER");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from shop where owner_user_id = ?", Integer.class, userId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from notification where user_id = ?", Integer.class, userId)).isEqualTo(0);
    }

    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\",\"nickname\":\"Failure Test\"}"))
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

    private void submit(String token) throws Exception {
        mockMvc.perform(post("/api/user/merchant-application")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"Failure Shop\",\"categoryId\":1,\"idCardNo\":\"110101199001011234\","
                                + "\"bankCardNo\":\"6222021234567890123\",\"licenseImg\":\"/license.png\","
                                + "\"warehouseAddr\":\"Warehouse\",\"warehouseProvince\":\"Guangdong\","
                                + "\"warehouseCity\":\"Shenzhen\",\"warehouseDetail\":\"Nanshan Road\","
                                + "\"contactName\":\"Applicant\",\"contactPhone\":\"13800138000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String uniqueUsername() {
        return "uc03-failure-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

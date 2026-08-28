package com.segroup8.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
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
@Tag("UC01")
class IdentityUc01IntegrationTest {

    private static final String PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    void registerLoginRoleBoundaryAndBanMustShareOnePersistedChain() throws Exception {
        String username = uniqueUsername("uc01");
        int usersBefore = countUsers(username);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username, "UC01 User")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Long userId = userId(username);
        String storedPassword = jdbcTemplate.queryForObject(
                "select password from user where id = ?", String.class, userId);
        assertThat(storedPassword).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(jdbcTemplate.queryForObject(
                "select role from user where id = ?", String.class, userId)).isEqualTo("USER");
        assertThat(jdbcTemplate.queryForObject(
                "select status from user where id = ?", String.class, userId)).isEqualTo("NORMAL");

        String userToken = login(username, PASSWORD);
        assertThat(jwtUtils.parseToken(userToken).get("uid").toString()).isEqualTo(userId.toString());

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        String adminToken = jwtUtils.createToken(2L, "admin1", "ADMIN");
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(put("/api/admin/users/{userId}/ban", userId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertThat(jdbcTemplate.queryForObject(
                "select status from user where id = ?", String.class, userId)).isEqualTo("BANNED");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
        assertThat(countUsers(username)).isEqualTo(usersBefore + 1);
    }

    @Test
    void duplicateInvalidAndWrongPasswordRequestsMustNotCreateDirtyUsers() throws Exception {
        String username = uniqueUsername("uc01-matrix");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username, "Matrix User")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        int usersAfterFirstRegister = countUsers(username);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username, "Duplicate User")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, "wrong-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"123\",\"nickname\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        assertThat(countUsers(username)).isEqualTo(usersAfterFirstRegister);
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("token");
        assertThat(token.asText()).isNotBlank();
        return token.asText();
    }

    private Long userId(String username) {
        return jdbcTemplate.queryForObject(
                "select id from user where username = ?", Long.class, username);
    }

    private int countUsers(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from user where username = ?", Integer.class, username);
        return count == null ? 0 : count;
    }

    private String uniqueUsername(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String registerJson(String username, String nickname) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD
                + "\",\"nickname\":\"" + nickname + "\",\"phone\":\"13800138000\","
                + "\"email\":\"" + username + "@example.com\"}";
    }

    private String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }
}

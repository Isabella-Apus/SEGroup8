package com.segroup8.identity.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Tag("MYSQL")
class MySqlMigrationIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("identity_governance_db")
            .withUsername("identity_governance_app")
            .withPassword("test-only-password");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired JdbcTemplate db;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void realMySqlRunsFlywayAndOwnsAllIdentityTables() {
        Long tables = db.queryForObject("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema=DATABASE() AND table_name IN ('user','address','merchant_application',"
                + "'user_report','user_block','credit_score_log','admin_audit_log','idempotency_record','outbox_event')",
                Long.class);
        assertThat(tables).isEqualTo(9);
    }

    @Test
    void realMySqlTraversesControllerJwtServiceJdbcAndFlywaySchema() throws Exception {
        byte[] registration = json.writeValueAsBytes(Map.of(
                "username", "mysql-api-user", "password", "User12345", "nickname", "MySQL User"));
        mvc.perform(post("/api/auth/register").contentType("application/json").content(registration))
                .andExpect(jsonPath("$.code").value(0));

        byte[] credentials = json.writeValueAsBytes(Map.of(
                "username", "mysql-api-user", "password", "User12345"));
        String loginBody = mvc.perform(post("/api/auth/login").contentType("application/json").content(credentials))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        JsonNode login = json.readTree(loginBody);
        String authorization = "Bearer " + login.path("data").path("token").asText();

        mvc.perform(put("/api/user/profile").header("Authorization", authorization)
                        .contentType("application/json")
                        .content(json.writeValueAsBytes(Map.of("nickname", "Stored In MySQL"))))
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/api/user/profile").header("Authorization", authorization))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("Stored In MySQL"));

        assertThat(db.queryForObject("SELECT nickname FROM `user` WHERE username=?", String.class,
                "mysql-api-user")).isEqualTo("Stored In MySQL");
    }
}

package com.segroup8.identity.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public abstract class IdentityTestSupport {
    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate db;
    @Autowired protected BCryptPasswordEncoder passwords;
    @Autowired protected ObjectMapper json;

    protected void resetDatabase() {
        db.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : new String[]{"outbox_event", "idempotency_record", "admin_audit_log",
                "credit_score_log", "user_block", "user_report", "report", "merchant_application",
                "address", "user"}) {
            db.execute("TRUNCATE TABLE `" + table + "`");
        }
        db.execute("SET REFERENTIAL_INTEGRITY TRUE");
        db.update("INSERT INTO `user`(username,password,nickname,role,status,credit_score,buyer_credit_score,seller_credit_score) "
                        + "VALUES(?,?,?,'ADMIN','NORMAL',100,100,100)",
                "admin", passwords.encode("admin123"), "管理员");
    }

    protected void register(String username) throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content(json.writeValueAsBytes(Map.of("username", username, "password", "User12345",
                        "nickname", username)))).andReturn();
    }

    protected Login login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsBytes(Map.of("username", username, "password", password))))
                .andReturn().getResponse().getContentAsString();
        JsonNode root = json.readTree(body);
        JsonNode data = root.path("data");
        return new Login(root.path("code").asInt(), data.path("token").asText(), data.path("user").path("id").asLong());
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected record Login(int code, String token, long userId) {
    }
}

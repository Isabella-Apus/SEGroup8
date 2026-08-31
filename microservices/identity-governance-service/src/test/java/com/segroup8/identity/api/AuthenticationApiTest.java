package com.segroup8.identity.api;

import com.segroup8.identity.support.IdentityTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("DOMAIN_A")
@Tag("UC01")
class AuthenticationApiTest extends IdentityTestSupport {
    @BeforeEach
    void setUp() {
        resetDatabase();
    }

    @Test
    void registerLoginValidationAndRoleBoundaryAreEnforced() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(json.writeValueAsBytes(Map.of("username", "alice", "password", "User12345",
                                "nickname", "Alice"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(json.writeValueAsBytes(Map.of("username", "alice", "password", "User12345",
                                "nickname", "Again"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(json.writeValueAsBytes(Map.of("username", "alice", "password", "wrong-pass"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));

        Login user = login("alice", "User12345");
        Login admin = login("admin", "admin123");
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(user.token())))
                .andExpect(jsonPath("$.code").value(403));
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(admin.token())))
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }
}

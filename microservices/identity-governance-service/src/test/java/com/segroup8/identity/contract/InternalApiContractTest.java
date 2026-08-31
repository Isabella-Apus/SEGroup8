package com.segroup8.identity.contract;

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

@SpringBootTest
@AutoConfigureMockMvc
@Tag("CONTRACT")
class InternalApiContractTest extends IdentityTestSupport {
    @BeforeEach
    void setUp() {
        resetDatabase();
    }

    @Test
    void serviceIdentityRequestIdAndIdempotencyKeyAreMandatory() throws Exception {
        register("internal-user");
        Login user = login("internal-user", "User12345");

        mvc.perform(get("/internal/users/{id}/summary", user.userId()))
                .andExpect(jsonPath("$.code").value(403));
        mvc.perform(get("/internal/users/{id}/summary", user.userId())
                        .header("X-Internal-Service-Token", "test-internal-service-token"))
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(get("/internal/users/{id}/summary", user.userId())
                        .header("X-Internal-Service-Token", "test-internal-service-token")
                        .header("X-Request-Id", "contract-1"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("internal-user"));

        mvc.perform(post("/internal/auth/introspect")
                        .header("X-Internal-Service-Token", "test-internal-service-token")
                        .header("X-Request-Id", "contract-2")
                        .contentType("application/json").content(json.writeValueAsBytes(Map.of("token", user.token()))))
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/internal/auth/introspect")
                        .header("X-Internal-Service-Token", "test-internal-service-token")
                        .header("X-Request-Id", "contract-3").header("X-Idempotency-Key", "idem-1")
                        .contentType("application/json").content(json.writeValueAsBytes(Map.of("token", user.token()))))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.active").value(true));
    }
}

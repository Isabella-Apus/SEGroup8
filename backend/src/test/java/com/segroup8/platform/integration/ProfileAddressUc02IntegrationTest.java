package com.segroup8.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("DOMAIN_A")
@Tag("UC02")
class ProfileAddressUc02IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    void profileAndAddressCrudMustPersistAndKeepOneDefaultPerUser() throws Exception {
        String username = uniqueUsername("uc02");
        register(username, "Profile User");
        String token = login(username);

        mockMvc.perform(get("/api/user/profile").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(username));

        mockMvc.perform(put("/api/user/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Updated UC02\",\"email\":\"updated@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/user/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("Updated UC02"))
                .andExpect(jsonPath("$.data.email").value("updated@example.com"));

        String firstAddress = addressJson("First Receiver", "First Street", 1);
        String secondAddress = addressJson("Second Receiver", "Second Street", 1);
        mockMvc.perform(post("/api/user/addresses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstAddress))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        Long firstId = latestAddressId(username);

        mockMvc.perform(post("/api/user/addresses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondAddress))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        Long secondId = latestAddressId(username);

        JsonNode listed = objectMapper.readTree(mockMvc.perform(get("/api/user/addresses")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString());
        assertThat(listed.path("data").size()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from address where user_id = (select id from user where username = ?) and is_default = 1",
                Integer.class, username)).isEqualTo(1);

        mockMvc.perform(put("/api/user/addresses/{id}", firstId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson("Updated Receiver", "Updated Street", 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(delete("/api/user/addresses/{id}", secondId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/user/addresses").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].receiverName").value("Updated Receiver"));
    }

    @Test
    void addressOwnershipMustPreventCrossUserUpdateAndDelete() throws Exception {
        String owner = uniqueUsername("uc02-owner");
        String other = uniqueUsername("uc02-other");
        register(owner, "Owner");
        register(other, "Other");
        String ownerToken = login(owner);
        String otherToken = login(other);

        mockMvc.perform(post("/api/user/addresses")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson("Owner", "Owner Street", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        Long ownerAddressId = latestAddressId(owner);

        mockMvc.perform(put("/api/user/addresses/{id}", ownerAddressId)
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson("Intruder", "Intruder Street", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(delete("/api/user/addresses/{id}", ownerAddressId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
        assertThat(jdbcTemplate.queryForObject(
                "select receiver_name from address where id = ?", String.class, ownerAddressId)).isEqualTo("Owner");

        mockMvc.perform(get("/api/user/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    private void register(String username, String nickname) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\","
                                + "\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String login(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private Long latestAddressId(String username) {
        return jdbcTemplate.queryForObject(
                "select max(id) from address where user_id = (select id from user where username = ?)",
                Long.class, username);
    }

    private String addressJson(String receiver, String detail, int isDefault) {
        return "{\"receiverName\":\"" + receiver + "\",\"receiverPhone\":\"13800138000\","
                + "\"province\":\"Guangdong\",\"city\":\"Shenzhen\",\"detailAddress\":\""
                + detail + "\",\"isDefault\":" + isDefault + "}";
    }

    private String uniqueUsername(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

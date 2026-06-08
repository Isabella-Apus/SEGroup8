package com.segroup8.platform.integration;

import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecondhandAuctionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    void sellerCanCreateAuctionWhenOnlyHistoricalAuctionExists() throws Exception {
        String sellerToken = jwtUtils.createToken(3L, "seller1", "OFFICIAL_SELLER");
        String payload = """
                {
                  "productId": 6,
                  "startPrice": 60.00,
                  "incrementAmount": 5.00,
                  "durationMinutes": 30
                }
                """;

        mockMvc.perform(post("/api/secondhand/trade/auction")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.productId").value(6))
                .andExpect(jsonPath("$.data.currentPrice").value(60.00))
                .andExpect(jsonPath("$.data.status").value("ONGOING"));
    }
}

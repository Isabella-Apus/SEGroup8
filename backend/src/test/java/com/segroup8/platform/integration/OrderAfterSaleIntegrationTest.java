package com.segroup8.platform.integration;

import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderAfterSaleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    void buyerApply_thenAdminApprove_thenLogsShouldExist() throws Exception {
        String adminToken = jwtUtils.createToken(2L, "admin1", "ADMIN");

        // 管理员同意（data-test.sql 中该订单 refund_status=1）
        mockMvc.perform(post("/api/admin/orders/101/refund/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"remark\":\"同意退款\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.refundDecisionSource").value("ADMIN"))
                .andExpect(jsonPath("$.data.refundDecisionRemark").value("同意退款"));

        // 查售后日志
        mockMvc.perform(get("/api/admin/orders/101/after-sale-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").exists());
    }
}


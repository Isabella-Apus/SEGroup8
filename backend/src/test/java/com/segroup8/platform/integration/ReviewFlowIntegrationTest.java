package com.segroup8.platform.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.entity.Review;
import com.segroup8.platform.mapper.ReviewMapper;
import com.segroup8.platform.testsupport.DomainCTestTags;
import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:integration/uc15-review-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Tag(DomainCTestTags.DOMAIN_C)
@Tag(DomainCTestTags.UC15)
class ReviewFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ReviewMapper reviewMapper;
    @Autowired private JwtUtils jwtUtils;

    @AfterEach
    void clearContext() { UserContext.clear(); }

    @Test
    void buyerOriginal_sellerReply_buyerFollowup_persistsAndIsIdempotent() throws Exception {
        UserContext.setUserId(1501L);
        mockMvc.perform(post("/api/order/1501/review").header("Authorization", bearer(1501L, "USER"))
                        .contentType("application/json")
                        .content("{\"score\":5,\"content\":\"首评内容\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));

        Review original = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, 1501L).eq(Review::getReviewType, "ORIGINAL").last("limit 1"));
        assertEquals("ORIGINAL", original.getReviewType());

        UserContext.setUserId(1502L);
        mockMvc.perform(post("/api/review/" + original.getId() + "/reply").header("Authorization", bearer(1502L, "OFFICIAL_SELLER"))
                        .contentType("application/json")
                        .content("{\"reply\":\"卖家回复\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));

        UserContext.setUserId(1501L);
        mockMvc.perform(post("/api/review/followup").header("Authorization", bearer(1501L, "USER"))
                        .contentType("application/json")
                        .content("{\"orderId\":1501,\"productType\":\"NEW\",\"productId\":1501,\"score\":4,\"content\":\"追评内容\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/api/review/followup").header("Authorization", bearer(1501L, "USER"))
                        .contentType("application/json")
                        .content("{\"orderId\":1501,\"productType\":\"NEW\",\"productId\":1501,\"score\":4,\"content\":\"重复追评\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));

        assertEquals(3, reviewMapper.selectCount(new LambdaQueryWrapper<Review>().eq(Review::getOrderId, 1501L)));
    }

    private String bearer(Long userId, String role) {
        return "Bearer " + jwtUtils.createToken(userId, "uc15-" + userId, role);
    }
}

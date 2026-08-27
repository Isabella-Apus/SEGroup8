package com.segroup8.platform.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.segroup8.platform.common.OrderStatusEnum;
import com.segroup8.platform.dto.OrderItemReviewBatchSubmitRequest;
import com.segroup8.platform.dto.OrderItemReviewSubmitRequest;
import com.segroup8.platform.entity.Review;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.ReviewMapper;
import com.segroup8.platform.service.OrderService;
import com.segroup8.platform.testsupport.DomainCTestTags;
import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Sql(scripts = "/integration/uc15-review-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Tag(DomainCTestTags.DOMAIN_C)
@Tag(DomainCTestTags.UC15)
class ReviewFlowIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.6")
            .withDatabaseName("segroup8_uc15")
            .withUsername("segroup8")
            .withPassword("segroup8_test")
            .withCommand("--log-bin-trust-function-creators=1")
            .withInitScript("schema.sql");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ReviewMapper reviewMapper;
    @Autowired private OrderInfoMapper orderInfoMapper;
    @Autowired private OrderService orderService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtUtils jwtUtils;

    @Test
    void buyerOriginal_sellerReply_buyerFollowup_persistsAndAllowsOnlyOneFollowup() throws Exception {
        mockMvc.perform(post("/api/review/followup").header("Authorization", bearer(1501L, "USER"))
                        .contentType("application/json")
                        .content(followupJson("首评前追评")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/order/1501/review").header("Authorization", bearer(1501L, "USER"))
                        .contentType("application/json")
                        .content("{\"score\":5,\"content\":\"首评内容\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").value(OrderStatusEnum.COMPLETED.getCode()));

        List<Review> originals = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, 1501L)
                .eq(Review::getReviewType, "ORIGINAL")
                .orderByAsc(Review::getProductId));
        assertThat(originals).extracting(Review::getProductId).containsExactly(1501L, 1502L);
        assertEquals(OrderStatusEnum.COMPLETED.getCode(), orderInfoMapper.selectById(1501L).getOrderStatus());

        mockMvc.perform(post("/api/review/" + originals.get(0).getId() + "/reply")
                        .header("Authorization", bearer(1503L, "OFFICIAL_SELLER"))
                        .contentType("application/json")
                        .content("{\"reply\":\"越权回复\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(post("/api/review/" + originals.get(0).getId() + "/reply")
                        .header("Authorization", bearer(1502L, "OFFICIAL_SELLER"))
                        .contentType("application/json")
                        .content("{\"reply\":\"卖家回复\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/review/followup").header("Authorization", bearer(1501L, "USER"))
                        .contentType("application/json")
                        .content(followupJson("追评内容")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/api/review/followup").header("Authorization", bearer(1501L, "USER"))
                        .contentType("application/json")
                        .content(followupJson("重复追评")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));

        assertEquals(3, reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, 1501L)));
        Review persistedOriginal = reviewMapper.selectById(originals.get(0).getId());
        assertEquals("卖家回复", persistedOriginal.getSellerReply());
        assertThat(persistedOriginal.getSellerReplyTime()).isNotNull();
    }

    @Test
    void secondItemDatabaseFailure_rollsBackFirstReview_andLeavesOrderPendingReview() {
        jdbcTemplate.execute("CREATE TRIGGER uc15_fail_second_review BEFORE INSERT ON review "
                + "FOR EACH ROW BEGIN IF NEW.product_id = 1502 THEN "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'forced second review failure'; END IF; END");
        try {
            OrderItemReviewBatchSubmitRequest request = new OrderItemReviewBatchSubmitRequest();
            request.setItems(List.of(
                    item(1501L, 5, "第一件商品"),
                    item(1502L, 4, "第二件商品")));
            com.segroup8.platform.context.UserContext.setUserId(1501L);
            assertThrows(RuntimeException.class,
                    () -> orderService.submitMyOrderItemReviews(1501L, request));
        } finally {
            com.segroup8.platform.context.UserContext.clear();
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS uc15_fail_second_review");
        }

        assertEquals(0, reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, 1501L)));
        assertEquals(OrderStatusEnum.RECEIVED.getCode(), orderInfoMapper.selectById(1501L).getOrderStatus());
    }

    @Test
    void buyerAndSellerPagination_filterBeforePaging_andNeverLeakOtherUsersData() throws Exception {
        jdbcTemplate.update("INSERT INTO review (order_id, product_type, product_id, user_id, score, content, review_type, status) "
                + "VALUES (1501, 'NEW', 1501, 1501, 5, 'own-one', 'ORIGINAL', 1), "
                + "(1501, 'NEW', 1502, 1501, 4, 'own-two', 'ORIGINAL', 1), "
                + "(1501, 'NEW', 1503, 1504, 1, 'other-seller-review', 'ORIGINAL', 1)");

        mockMvc.perform(get("/api/review/my")
                        .header("Authorization", bearer(1501L, "USER"))
                        .param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[?(@.userId == 1504)]").isEmpty());

        mockMvc.perform(get("/api/review/seller/list")
                        .header("Authorization", bearer(1502L, "OFFICIAL_SELLER"))
                        .param("pageNum", "1").param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].productId").value(1502));
        mockMvc.perform(get("/api/review/seller/list")
                        .header("Authorization", bearer(1502L, "OFFICIAL_SELLER"))
                        .param("pageNum", "2").param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].productId").value(1501));
    }

    private String followupJson(String content) {
        return "{\"orderId\":1501,\"productType\":\"NEW\",\"productId\":1501,"
                + "\"score\":4,\"content\":\"" + content + "\"}";
    }

    private OrderItemReviewSubmitRequest item(Long productId, int score, String content) {
        OrderItemReviewSubmitRequest item = new OrderItemReviewSubmitRequest();
        item.setProductType("NEW");
        item.setProductId(productId);
        item.setScore(score);
        item.setContent(content);
        return item;
    }

    private String bearer(Long userId, String role) {
        return "Bearer " + jwtUtils.createToken(userId, "uc15-" + userId, role);
    }
}

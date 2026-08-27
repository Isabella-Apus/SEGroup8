package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.segroup8.platform.common.OrderStatusEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.OrderItemReviewBatchSubmitRequest;
import com.segroup8.platform.dto.OrderItemReviewSubmitRequest;
import com.segroup8.platform.dto.OrderReviewSubmitRequest;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.Review;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.ReviewMapper;
import com.segroup8.platform.service.OrderService;
import com.segroup8.platform.testsupport.DomainCTestTags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "classpath:integration/uc15-review-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Tag(DomainCTestTags.DOMAIN_C)
@Tag(DomainCTestTags.UC15)
class ReviewServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void orderReview_writesOneOriginalForEveryOrderItem_andCompletesOrder() {
        UserContext.setUserId(1501L);
        OrderReviewSubmitRequest request = new OrderReviewSubmitRequest();
        request.setScore(5);
        request.setContent("两件商品都满意");
        orderService.submitMyOrderReview(1501L, request);

        assertEquals(2, reviewMapper.selectCount(new LambdaQueryWrapper<Review>().eq(Review::getOrderId, 1501L)));
        assertEquals(OrderStatusEnum.COMPLETED.getCode(), orderInfoMapper.selectById(1501L).getOrderStatus());
    }

    @Test
    void duplicateOriginalReview_isRejected() {
        UserContext.setUserId(1501L);
        OrderReviewSubmitRequest request = new OrderReviewSubmitRequest();
        request.setScore(4);
        request.setContent("首评");
        orderService.submitMyOrderReview(1501L, request);
        assertThrows(RuntimeException.class, () -> orderService.submitMyOrderReview(1501L, request));
    }

    @Test
    void invalidItemInBatch_isRejectedBeforeAnyInsert_andOrderStaysPendingReview() {
        UserContext.setUserId(1501L);
        OrderItemReviewSubmitRequest valid = item(1501L, 5, "商品一");
        OrderItemReviewSubmitRequest invalid = item(9999L, 1, "不属于订单");
        OrderItemReviewBatchSubmitRequest request = new OrderItemReviewBatchSubmitRequest();
        request.setItems(List.of(valid, invalid));
        assertThrows(RuntimeException.class, () -> orderService.submitMyOrderItemReviews(1501L, request));
        assertEquals(0, reviewMapper.selectCount(new LambdaQueryWrapper<Review>().eq(Review::getOrderId, 1501L)));
        assertEquals(OrderStatusEnum.RECEIVED.getCode(), orderInfoMapper.selectById(1501L).getOrderStatus());
    }

    private OrderItemReviewSubmitRequest item(Long productId, int score, String content) {
        OrderItemReviewSubmitRequest item = new OrderItemReviewSubmitRequest();
        item.setProductType("NEW");
        item.setProductId(productId);
        item.setScore(score);
        item.setContent(content);
        return item;
    }
}

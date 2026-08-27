package com.segroup8.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.entity.Review;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.ReviewMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.testsupport.DomainCTestTags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag(DomainCTestTags.DOMAIN_C)
@Tag(DomainCTestTags.UC15)
class ReviewControllerWebMvcTest {

    private MockMvc mockMvc;
    @Mock private ReviewMapper reviewMapper;
    @Mock private OrderInfoMapper orderInfoMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private ProductMapper productMapper;
    @Mock private ShopMapper shopMapper;
    @Mock private SecondhandProductMapper secondhandProductMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(
                        reviewMapper, orderInfoMapper, orderItemMapper, productMapper, shopMapper, secondhandProductMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() { UserContext.clear(); }

    @Test
    void sellerReply_requiresOwnershipAndValidContent() throws Exception {
        UserContext.setUserId(1502L);
        Review review = new Review();
        review.setId(1L);
        review.setProductType("NEW");
        review.setProductId(1501L);
        when(reviewMapper.selectById(1L)).thenReturn(review);
        Product product = new Product(); product.setId(1501L); product.setShopId(1501L);
        Shop shop = new Shop(); shop.setId(1501L); shop.setOwnerUserId(1502L);
        when(productMapper.selectById(1501L)).thenReturn(product);
        when(shopMapper.selectById(1501L)).thenReturn(shop);

        mockMvc.perform(post("/api/review/1/reply").contentType("application/json")
                        .content("{\"reply\":\"感谢反馈，我们已处理\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        verify(reviewMapper).updateById(any(Review.class));

        UserContext.setUserId(1503L);
        mockMvc.perform(post("/api/review/1/reply").contentType("application/json")
                        .content("{\"reply\":\"越权\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
        UserContext.setUserId(1502L);
        mockMvc.perform(post("/api/review/1/reply").contentType("application/json")
                        .content("{\"reply\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void myReviews_areScopedToCurrentUser() throws Exception {
        UserContext.setUserId(1501L);
        Review review = new Review();
        review.setId(7L); review.setOrderId(1501L); review.setUserId(1501L);
        review.setProductType("NEW"); review.setProductId(1501L); review.setReviewType("ORIGINAL");
        Page<Review> page = new Page<>(1, 10); page.setRecords(List.of(review)); page.setTotal(1);
        when(reviewMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(orderInfoMapper.selectById(1501L)).thenReturn(new OrderInfo());
        when(orderItemMapper.selectOne(any())).thenReturn(new OrderItem());

        mockMvc.perform(get("/api/review/my").param("keyword", "")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.total").value(1));
        verify(reviewMapper).selectPage(any(Page.class), any());
    }

    @Test
    void sellerReviews_withoutOwnedProducts_returnsEmptyWithoutQueryingUnscopedReviews() throws Exception {
        UserContext.setUserId(1503L);
        when(shopMapper.selectList(any())).thenReturn(List.of());
        when(secondhandProductMapper.selectList(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/review/seller/list")
                        .param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isEmpty());

        verify(reviewMapper, never()).selectPage(any(Page.class), any());
    }
}

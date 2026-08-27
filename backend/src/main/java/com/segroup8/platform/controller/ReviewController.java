package com.segroup8.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.Result;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.ReviewPageQueryRequest;
import com.segroup8.platform.dto.OrderReviewFollowUpSubmitRequest;
import com.segroup8.platform.dto.ReviewReplyRequest;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.entity.Review;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.ReviewMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.List;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewMapper reviewMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ShopMapper shopMapper;
    private final SecondhandProductMapper secondhandProductMapper;

    public ReviewController(ReviewMapper reviewMapper, OrderInfoMapper orderInfoMapper, OrderItemMapper orderItemMapper,
            ProductMapper productMapper, ShopMapper shopMapper, SecondhandProductMapper secondhandProductMapper) {
        this.reviewMapper = reviewMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.shopMapper = shopMapper;
        this.secondhandProductMapper = secondhandProductMapper;
    }

    @Operation(summary = "分页查询我的评价")
    @GetMapping("/my")
    public Result<PageVO<ReviewVO>> pageMyReviews(@Valid @ModelAttribute ReviewPageQueryRequest request) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        String keyword = request.getKeyword() == null ? null : request.getKeyword().trim();
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(request.getScore() != null, Review::getScore, request.getScore())
                .like(StringUtils.hasText(keyword), Review::getContent, keyword)
                .orderByDesc(Review::getCreateTime);

        if (request.getStartTime() != null) {
            LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getStartTime()), ZoneId.of("Asia/Shanghai"));
            wrapper.ge(Review::getCreateTime, start);
        }
        if (request.getEndTime() != null) {
            LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getEndTime()), ZoneId.of("Asia/Shanghai"));
            wrapper.le(Review::getCreateTime, end);
        }

        Page<Review> page = reviewMapper.selectPage(Page.of(request.getPageNum(), request.getPageSize()), wrapper);
        List<ReviewVO> records = page.getRecords().stream().map(this::toVO).toList();
        PageVO<ReviewVO> vo = new PageVO<>();
        vo.setTotal(page.getTotal());
        vo.setPageNum(page.getCurrent());
        vo.setPageSize(page.getSize());
        vo.setRecords(records);
        return Result.success(vo);
    }

    private ReviewVO toVO(Review review) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setOrderId(review.getOrderId());
        vo.setProductType(review.getProductType());
        vo.setProductId(review.getProductId());
        vo.setUserId(review.getUserId());
        vo.setScore(review.getScore());
        vo.setContent(review.getContent());
        vo.setReviewType(review.getReviewType());
        vo.setSellerReply(review.getSellerReply());
        vo.setSellerReplyTime(review.getSellerReplyTime());
        vo.setCreateTime(review.getCreateTime());

        OrderInfo order = orderInfoMapper.selectById(review.getOrderId());
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
        }
        OrderItem item = orderItemMapper.selectOne(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, review.getOrderId())
                .eq(OrderItem::getProductType, review.getProductType())
                .eq(OrderItem::getProductId, review.getProductId())
                .last("limit 1"));
        if (item != null) {
            vo.setProductName(item.getProductName());
        }
        return vo;
    }

    @Operation(summary = "卖家分页查询评价（仅自己商品）")
    @GetMapping("/seller/list")
    public Result<PageVO<ReviewVO>> pageSellerReviews(@Valid @ModelAttribute ReviewPageQueryRequest request) {
        Long sellerUserId = UserContext.getUserId();
        if (sellerUserId == null) {
            throw new BusinessException(401, "未登录");
        }
        String keyword = request.getKeyword() == null ? null : request.getKeyword().trim();
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>()
                .eq(request.getScore() != null, Review::getScore, request.getScore())
                .like(StringUtils.hasText(keyword), Review::getContent, keyword)
                .orderByDesc(Review::getCreateTime);
        if (request.getStartTime() != null) {
            LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getStartTime()), ZoneId.of("Asia/Shanghai"));
            wrapper.ge(Review::getCreateTime, start);
        }
        if (request.getEndTime() != null) {
            LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getEndTime()), ZoneId.of("Asia/Shanghai"));
            wrapper.le(Review::getCreateTime, end);
        }

        Page<Review> page = reviewMapper.selectPage(Page.of(request.getPageNum(), request.getPageSize()), wrapper);
        List<ReviewVO> records = page.getRecords().stream()
                .filter(r -> isOwnedBySeller(r, sellerUserId))
                .map(this::toVO)
                .toList();

        PageVO<ReviewVO> vo = new PageVO<>();
        vo.setTotal((long) records.size());
        vo.setPageNum(page.getCurrent());
        vo.setPageSize(page.getSize());
        vo.setRecords(records);
        return Result.success(vo);
    }

    @Operation(summary = "卖家回复评价")
    @PostMapping("/{reviewId}/reply")
    public Result<Void> reply(@PathVariable Long reviewId, @Valid @RequestBody ReviewReplyRequest request) {
        Long sellerUserId = UserContext.getUserId();
        if (sellerUserId == null) {
            throw new BusinessException(401, "未登录");
        }
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(404, "评价不存在");
        }
        if (!isOwnedBySeller(review, sellerUserId)) {
            throw new BusinessException(403, "无权回复该评价");
        }
        review.setSellerReply(request.getReply().trim());
        review.setSellerReplyTime(LocalDateTime.now());
        reviewMapper.updateById(review);
        return Result.success();
    }

    private boolean isOwnedBySeller(Review review, Long sellerUserId) {
        if (review == null || sellerUserId == null || !StringUtils.hasText(review.getProductType()) || review.getProductId() == null) {
            return false;
        }
        if ("NEW".equalsIgnoreCase(review.getProductType())) {
            Product product = productMapper.selectById(review.getProductId());
            if (product == null) {
                return false;
            }
            Shop shop = shopMapper.selectById(product.getShopId());
            return shop != null && Objects.equals(shop.getOwnerUserId(), sellerUserId);
        }
        if ("SECONDHAND".equalsIgnoreCase(review.getProductType())) {
            SecondhandProduct secondhand = secondhandProductMapper.selectById(review.getProductId());
            return secondhand != null && Objects.equals(secondhand.getSellerUserId(), sellerUserId);
        }
        return false;
    }

    @Operation(summary = "追评")
    @PostMapping("/followup")
    public Result<Void> followup(@Valid @RequestBody OrderReviewFollowUpSubmitRequest request) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }

        OrderInfo order = orderInfoMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!userId.equals(order.getBuyerUserId())) {
            throw new BusinessException(403, "无权操作该订单");
        }

        boolean originalExists = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, request.getOrderId())
                .eq(Review::getUserId, userId)
                .eq(Review::getProductType, request.getProductType())
                .eq(Review::getProductId, request.getProductId())
                .eq(Review::getReviewType, "ORIGINAL")) > 0;
        if (!originalExists) {
            throw new BusinessException(400, "原评价不存在，无法追评");
        }

        boolean followupExists = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, request.getOrderId())
                .eq(Review::getUserId, userId)
                .eq(Review::getProductType, request.getProductType())
                .eq(Review::getProductId, request.getProductId())
                .eq(Review::getReviewType, "FOLLOWUP")) > 0;
        if (followupExists) {
            throw new BusinessException(400, "该商品已追评过，不可重复追评");
        }

        Review followup = new Review();
        followup.setOrderId(request.getOrderId());
        followup.setProductType(request.getProductType());
        followup.setProductId(request.getProductId());
        followup.setUserId(userId);
        followup.setScore(request.getScore());
        followup.setContent(request.getContent());
        followup.setReviewType("FOLLOWUP");
        followup.setStatus(1);
        reviewMapper.insert(followup);
        return Result.success();
    }
}

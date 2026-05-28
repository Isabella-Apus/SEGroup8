package com.segroup8.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.segroup8.platform.common.OrderStatusEnum;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.Review;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.ReviewMapper;
import com.segroup8.platform.vo.CreditScoreVO;
import com.segroup8.platform.vo.SellerRatingVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class SellerRatingAssembler {

    private final CreditService creditService;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ReviewMapper reviewMapper;

    public SellerRatingAssembler(CreditService creditService, OrderInfoMapper orderInfoMapper,
            OrderItemMapper orderItemMapper, ReviewMapper reviewMapper) {
        this.creditService = creditService;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.reviewMapper = reviewMapper;
    }

    public SellerRatingVO buildForShop(Long userId) {
        SellerRatingVO rating = buildBase(userId);
        if (rating == null) {
            return null;
        }
        Integer performanceScore = calculateShopPerformanceScore(userId);
        rating.setOverallScore(performanceScore);
        rating.setOverallLevel(toPerformanceLevel(performanceScore));
        return rating;
    }

    public SellerRatingVO buildForSecondhand(Long userId) {
        SellerRatingVO rating = buildBase(userId);
        if (rating == null) {
            return null;
        }
        Integer sellerRatingScore = calculateSecondhandSellerRatingScore(userId);
        rating.setShSellerRatingScore(sellerRatingScore);
        rating.setShSellerRatingLevel(toCreditLevel(sellerRatingScore));
        return rating;
    }

    private SellerRatingVO buildBase(Long userId) {
        if (userId == null) {
            return null;
        }
        CreditScoreVO credit = creditService.getCreditInfo(userId);
        SellerRatingVO rating = new SellerRatingVO();
        rating.setOverallScore(credit.getOverallScore());
        rating.setOverallLevel(credit.getOverallLevel());
        rating.setBuyerScore(credit.getBuyerScore());
        rating.setBuyerLevel(credit.getBuyerLevel());
        rating.setShopScore(credit.getShopScore());
        rating.setShopLevel(credit.getShopLevel());
        rating.setShopSoldCount(credit.getShopSoldCount());
        rating.setShopGoodRate(credit.getShopGoodRate());
        rating.setShSellerScore(credit.getShSellerScore());
        rating.setShSellerLevel(credit.getShSellerLevel());
        rating.setShSellerSoldCount(credit.getShSellerSoldCount());
        rating.setShSellerGoodRate(credit.getShSellerGoodRate());
        return rating;
    }

    private Integer calculateShopPerformanceScore(Long sellerUserId) {
        List<Long> orderIds = listShopOrderIds(sellerUserId);
        List<OrderInfo> orders = orderIds.isEmpty()
                ? List.of()
                : orderInfoMapper.selectList(new QueryWrapper<OrderInfo>().in("id", orderIds));

        long paidOrders = orders.stream()
                .filter(order -> Integer.valueOf(1).equals(order.getPayStatus()))
                .count();
        long completedOrders = orders.stream()
                .filter(order -> Integer.valueOf(OrderStatusEnum.COMPLETED.getCode()).equals(order.getOrderStatus()))
                .count();
        long refundOrders = orders.stream()
                .filter(order -> order.getRefundStatus() != null && order.getRefundStatus() > 0)
                .count();
        long shippedOrders = orders.stream()
                .filter(order -> order.getOrderStatus() != null && order.getOrderStatus() >= OrderStatusEnum.SHIPPED.getCode())
                .count();

        long reviewCount = reviewMapper.selectCount(shopReviewWrapper(sellerUserId));
        long positiveReviews = reviewCount == 0
                ? 0
                : reviewMapper.selectCount(shopReviewWrapper(sellerUserId).ge("score", 4));

        int positiveRate = reviewCount == 0 ? 100 : (int) Math.round(positiveReviews * 100.0 / reviewCount);
        int refundRate = paidOrders == 0 ? 0 : (int) Math.round(refundOrders * 100.0 / paidOrders);
        int shipRate = paidOrders == 0 ? 100 : (int) Math.round(shippedOrders * 100.0 / paidOrders);
        int completionRate = paidOrders == 0 ? 0 : (int) Math.round(completedOrders * 100.0 / paidOrders);

        double score = positiveRate * 0.4
                + (100 - refundRate) * 0.3
                + shipRate * 0.2
                + completionRate * 0.1;
        return Math.min(100, (int) Math.round(score));
    }

    private Integer calculateSecondhandSellerRatingScore(Long sellerUserId) {
        List<Review> reviews = reviewMapper.selectList(new QueryWrapper<Review>()
                .eq("product_type", "SECONDHAND")
                .inSql("product_id", "SELECT id FROM secondhand_product WHERE seller_user_id=" + sellerUserId));
        if (reviews.isEmpty()) {
            return null;
        }
        double averageScore = reviews.stream()
                .map(Review::getScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
        return Math.min(100, (int) Math.round(averageScore * 20));
    }

    private List<Long> listShopOrderIds(Long sellerUserId) {
        return orderItemMapper.selectList(new QueryWrapper<OrderItem>()
                        .select("order_id")
                        .eq("product_type", "NEW")
                        .inSql("product_id", shopProductSubQuery(sellerUserId)))
                .stream()
                .map(OrderItem::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private QueryWrapper<Review> shopReviewWrapper(Long sellerUserId) {
        return new QueryWrapper<Review>()
                .eq("product_type", "NEW")
                .inSql("product_id", shopProductSubQuery(sellerUserId));
    }

    private String shopProductSubQuery(Long sellerUserId) {
        return "SELECT id FROM product WHERE shop_id IN "
                + "(SELECT id FROM shop WHERE owner_user_id=" + sellerUserId + ")";
    }

    private String toPerformanceLevel(Integer score) {
        if (score == null) {
            return "暂无等级";
        }
        if (score >= 80) {
            return "优秀";
        }
        if (score >= 60) {
            return "良好";
        }
        return "待改善";
    }

    private String toCreditLevel(Integer score) {
        if (score == null) {
            return "暂无等级";
        }
        if (score >= 90) {
            return "极好";
        }
        if (score >= 70) {
            return "优秀";
        }
        if (score >= 50) {
            return "良好";
        }
        return "较差";
    }
}

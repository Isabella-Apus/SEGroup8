package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.segroup8.platform.common.AccessControl;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.OrderStatusEnum;
import com.segroup8.platform.entity.*;
import com.segroup8.platform.mapper.*;
import com.segroup8.platform.service.CreditService;
import com.segroup8.platform.vo.CreditScoreVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CreditServiceImpl implements CreditService {

    private final UserMapper userMapper;
    private final CreditScoreLogMapper creditScoreLogMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ReviewMapper reviewMapper;
    private final UserReportMapper userReportMapper;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public CreditServiceImpl(UserMapper userMapper,
                              CreditScoreLogMapper creditScoreLogMapper,
                              OrderInfoMapper orderInfoMapper,
                              OrderItemMapper orderItemMapper,
                              ReviewMapper reviewMapper,
                              UserReportMapper userReportMapper) {
        this.userMapper = userMapper;
        this.creditScoreLogMapper = creditScoreLogMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.reviewMapper = reviewMapper;
        this.userReportMapper = userReportMapper;
    }

    // ==================== 公开接口 ====================

    @Override
    public CreditScoreVO getCreditInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        return buildVO(user);
    }

    @Override
    public CreditScoreVO getMyCredit() {
        Long userId = AccessControl.requireUserId();
        return getCreditInfo(userId);
    }

    @Override
    @Transactional
    public void onOrderComplete(Long orderId, Long buyerUserId, Long sellerUserId) {
        // 幂等：同一订单已处理过则跳过
        if (creditScoreLogMapper.countByReasonAndRef(
                buyerUserId, "BUYER", "ORDER_COMPLETE", orderId) > 0) {
            return;
        }

        // -------- 买家加分 --------
        // 判断是否首单
        long buyerHistory = creditScoreLogMapper.selectCount(
                new LambdaQueryWrapper<CreditScoreLog>()
                        .eq(CreditScoreLog::getUserId, buyerUserId)
                        .eq(CreditScoreLog::getRole, "BUYER")
                        .eq(CreditScoreLog::getReasonCode, "ORDER_COMPLETE"));
        if (buyerHistory == 0) {
            // 首单：基础+2 + 首单奖励+3，写两条日志方便用户查看
            applyScore(buyerUserId, "BUYER", 2, "ORDER_COMPLETE",
                    "完成交易加分", orderId, null);
            applyScore(buyerUserId, "BUYER", 3, "FIRST_TRADE",
                    "首次成功交易奖励", orderId, null);
        } else {
            applyScore(buyerUserId, "BUYER", 2, "ORDER_COMPLETE",
                    "完成交易加分", orderId, null);
        }

        // -------- 卖家加分 --------
        int sellerDelta = 2; // 基础分

        // 该订单是否有5星好评（买家给卖家的评价）
        boolean hasFiveStar = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getOrderId, orderId)
                        .eq(Review::getUserId, buyerUserId)   // 买家写的
                        .eq(Review::getScore, 5)) > 0;
        if (hasFiveStar) sellerDelta += 1;

        // 卖家好评率>=95% 且售出>=10单
        int soldCount = countSellerSoldOrders(sellerUserId);
        double goodRate = calcSellerGoodRate(sellerUserId, soldCount);
        if (soldCount >= 10 && goodRate >= 95.0) sellerDelta += 1;

        applyScore(sellerUserId, "SELLER", sellerDelta, "ORDER_COMPLETE",
                "完成交易加分", orderId, null);
    }

    @Override
    @Transactional
    public void onBuyerDisputePenalty(Long orderId, Long buyerUserId, String reasonCode) {
        int delta;
        String desc;
        switch (reasonCode) {
            case "REFUND_ABUSE":
                delta = -10; desc = "恶意退款扣分"; break;
            case "CANCEL_OFTEN":
                delta = -5;  desc = "频繁取消订单扣分"; break;
            default:
                delta = -5;  desc = "订单纠纷判定买家责任扣分"; break;
        }
        applyScore(buyerUserId, "BUYER", delta, reasonCode, desc, orderId, null);
    }

    @Override
    @Transactional
    public void onSellerDisputePenalty(Long orderId, Long sellerUserId, String reasonCode) {
        int delta;
        String desc;
        switch (reasonCode) {
            case "FAKE_ITEM":
                delta = -8; desc = "商品与描述不符扣分"; break;
            default:
                delta = -5; desc = "订单纠纷判定卖家责任扣分"; break;
        }
        applyScore(sellerUserId, "SELLER", delta, reasonCode, desc, orderId, null);
    }

    @Override
    @Transactional
    public void onReportUpheld(Long reportId, Long reportedId, String reportedRole,
                                String reasonType, Integer customDelta) {
        int delta = (customDelta != null) ? -Math.abs(customDelta) : calcReportPenalty(reasonType);

        // 近2年被判成立>=3次，额外扣5分
        int upheldCount = userReportMapper.countUpheldReportsIn2Years(reportedId);
        if (upheldCount >= 3) delta -= 5;

        applyScore(reportedId, reportedRole, delta, "REPORT_UPHELD",
                "举报成立扣分：" + reasonType, reportId, null);
    }

    @Override
    @Transactional
    public void adminAdjust(Long userId, String role, int delta, String remark, Long adminId) {
        applyScore(userId, role, delta, "ADMIN_ADJUST", remark, null, adminId);
    }

    @Override
    public String calcLevel(int score) {
        if (score >= 90) return "极好";
        if (score >= 70) return "优秀";
        if (score >= 50) return "良好";
        return "较差";
    }

    // ==================== 私有方法 ====================

    /**
     * 核心：更新user表分数 + 写日志（带上下限0~100保护）
     * 你们User表只有一个creditScore，我们用BUYER身份更新它，
     * SELLER身份更新seller_credit_score（schema.sql里已追加的列）
     */
    private void applyScore(Long userId, String role, int delta,
                             String reasonCode, String reasonDesc,
                             Long refId, Long operatorId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        int current;
        if ("BUYER".equals(role)) {
            // 买家信用分 = 原有的 creditScore 字段
            current = user.getCreditScore() == null ? 100 : user.getCreditScore();
        } else {
            // 卖家信用分 = 新增的 seller_credit_score 字段
            current = user.getSellerCreditScore() == null ? 100 : user.getSellerCreditScore();
        }

        int newScore = Math.min(100, Math.max(0, current + delta));
        int actualDelta = newScore - current;
        if (actualDelta == 0) return; // 边界无变化

        if ("BUYER".equals(role)) {
            user.setCreditScore(newScore);
        } else {
            user.setSellerCreditScore(newScore);
        }
        userMapper.updateById(user);

        // 写日志
        CreditScoreLog log = new CreditScoreLog();
        log.setUserId(userId);
        log.setRole(role);
        log.setDelta(actualDelta);
        log.setReasonCode(reasonCode);
        log.setReasonDesc(reasonDesc);
        log.setRefId(refId);
        log.setOperatorId(operatorId);
        creditScoreLogMapper.insert(log);
    }

    private int calcReportPenalty(String reasonType) {
        switch (reasonType) {
            case "FRAUD":        return -20;
            case "REFUND_ABUSE": return -10;
            case "FAKE_ITEM":    return -8;
            case "SPAM":         return -6;
            case "BAD_ATTITUDE": return -5;
            default:             return -3;
        }
    }

    private CreditScoreVO buildVO(User user) {
        CreditScoreVO vo = new CreditScoreVO();
        Long uid = user.getId();

        // 买家
        int buyerScore = user.getCreditScore() == null ? 100 : user.getCreditScore();
        vo.setBuyerScore(buyerScore);
        vo.setBuyerLevel(calcLevel(buyerScore));
        int buyerOrderCount = countBuyerOrders(uid);
        vo.setBuyerOrderCount(buyerOrderCount);
        int buyerGoodReview = countBuyerGoodReviews(uid);
        vo.setBuyerGoodReviewCount(buyerGoodReview);
        vo.setBuyerDisputeCount(userReportMapper.countUpheldReportsIn2Years(uid));
        vo.setBuyerGoodRate(buyerOrderCount == 0 ? 100.0
                : Math.round(buyerGoodReview * 1000.0 / buyerOrderCount) / 10.0);

        // 卖家
        int sellerScore = user.getSellerCreditScore() == null ? 100 : user.getSellerCreditScore();
        vo.setSellerScore(sellerScore);
        vo.setSellerLevel(calcLevel(sellerScore));
        int soldCount = countSellerSoldOrders(uid);
        vo.setSellerSoldCount(soldCount);
        int sellerGoodReview = countSellerGoodReviews(uid);
        vo.setSellerGoodReviewCount(sellerGoodReview);
        vo.setSellerDisputeCount(userReportMapper.countUpheldReportsIn2Years(uid));
        vo.setSellerGoodRate(calcSellerGoodRate(uid, soldCount));

        // 最近10条日志
        vo.setBuyerLogs(buildLogVOs(uid, "BUYER"));
        vo.setSellerLogs(buildLogVOs(uid, "SELLER"));

        return vo;
    }

    private List<CreditScoreVO.CreditLogItemVO> buildLogVOs(Long userId, String role) {
        return creditScoreLogMapper.recentLogs(userId, role, 10)
                .stream().map(log -> {
                    CreditScoreVO.CreditLogItemVO item = new CreditScoreVO.CreditLogItemVO();
                    item.setDelta(log.getDelta());
                    item.setReasonDesc(log.getReasonDesc() != null
                            ? log.getReasonDesc() : codeToDesc(log.getReasonCode()));
                    item.setCreateTime(log.getCreateTime() != null
                            ? log.getCreateTime().format(FMT) : "");
                    return item;
                }).collect(Collectors.toList());
    }

    private String codeToDesc(String code) {
        if (code == null) return "";
        switch (code) {
            case "ORDER_COMPLETE":  return "完成交易加分";
            case "FIRST_TRADE":     return "首次成功交易奖励";
            case "REPORT_UPHELD":   return "举报成立扣分";
            case "ORDER_DISPUTE":   return "订单纠纷扣分";
            case "REFUND_ABUSE":    return "恶意退款扣分";
            case "CANCEL_OFTEN":    return "频繁取消订单扣分";
            case "FAKE_ITEM":       return "商品与描述不符扣分";
            case "ADMIN_ADJUST":    return "管理员调整";
            default:                return code;
        }
    }

    // -------- 统计辅助 --------

    private int countBuyerOrders(Long userId) {
        return orderInfoMapper.selectCount(
                new QueryWrapper<OrderInfo>()
                        .eq("buyer_user_id", userId)
                        .eq("order_status", OrderStatusEnum.COMPLETED.getCode())).intValue();
    }

    private int countBuyerGoodReviews(Long userId) {
        // Review.userId = 评价人，reviewType=BUYER_TO_SELLER 是买家写的
        return reviewMapper.selectCount(
                new QueryWrapper<Review>()
                        .eq("user_id", userId)
                        .eq("review_type", "BUYER_TO_SELLER")
                        .ge("score", 4)).intValue();
    }

    private int countSellerSoldOrders(Long sellerUserId) {
        // OrderInfo没有seller字段，通过OrderItem找卖家商品对应的订单
        List<Long> orderIds = orderItemMapper.selectList(
                        new QueryWrapper<OrderItem>()
                                .select("order_id")
                                .inSql("product_id",
                                        "SELECT id FROM product WHERE shop_id IN (SELECT id FROM shop WHERE owner_user_id=" + sellerUserId + ")"
                                        + " UNION SELECT id FROM secondhand_product WHERE seller_user_id=" + sellerUserId))
                .stream()
                .map(OrderItem::getOrderId)
                .distinct()
                .collect(Collectors.toList());
        if (orderIds.isEmpty()) return 0;
        return orderInfoMapper.selectCount(
                new QueryWrapper<OrderInfo>()
                        .in("id", orderIds)
                        .eq("order_status", OrderStatusEnum.COMPLETED.getCode())).intValue();
    }

    private int countSellerGoodReviews(Long sellerUserId) {
        // 找卖家的所有订单，再找这些订单里买家给的好评
        List<Long> orderIds = orderItemMapper.selectList(
                        new QueryWrapper<OrderItem>()
                                .select("order_id")
                                .inSql("product_id",
                                        "SELECT id FROM product WHERE shop_id IN (SELECT id FROM shop WHERE owner_user_id=" + sellerUserId + ")"
                                        + " UNION SELECT id FROM secondhand_product WHERE seller_user_id=" + sellerUserId))
                .stream()
                .map(OrderItem::getOrderId)
                .distinct()
                .collect(Collectors.toList());
        if (orderIds.isEmpty()) return 0;
        return reviewMapper.selectCount(
                new QueryWrapper<Review>()
                        .in("order_id", orderIds)
                        .eq("review_type", "BUYER_TO_SELLER")
                        .ge("score", 4)).intValue();
    }

    private double calcSellerGoodRate(Long sellerUserId, int soldCount) {
        if (soldCount == 0) return 100.0;
        int good = countSellerGoodReviews(sellerUserId);
        return Math.round(good * 1000.0 / soldCount) / 10.0;
    }

    private List<Long> getSellerProductIds(Long sellerUserId) {
        return orderItemMapper.selectList(
                        new QueryWrapper<OrderItem>()
                                .select("distinct product_id")
                                .inSql("product_id",
                                        "SELECT id FROM product WHERE shop_id IN (SELECT id FROM shop WHERE owner_user_id=" + sellerUserId + ")"
                                        + " UNION SELECT id FROM secondhand_product WHERE seller_user_id=" + sellerUserId))
                .stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());
    }




}
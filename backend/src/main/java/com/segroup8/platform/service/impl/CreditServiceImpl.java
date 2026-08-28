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

import java.time.LocalDateTime;
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
        // 幂等：同一订单买家已处理过则跳过
        if (creditScoreLogMapper.countByReasonAndRef(
                buyerUserId, "BUYER", "ORDER_COMPLETE", orderId) > 0) {
            return;
        }

        // -------- 买家加分（credit_score） --------
        long buyerHistory = creditScoreLogMapper.selectCount(
                new LambdaQueryWrapper<CreditScoreLog>()
                        .eq(CreditScoreLog::getUserId, buyerUserId)
                        .eq(CreditScoreLog::getRole, "BUYER")
                        .eq(CreditScoreLog::getReasonCode, "ORDER_COMPLETE"));
        if (buyerHistory == 0) {
            // 首单：基础+2 + 首单奖励+3
            applyScore(buyerUserId, "BUYER", 2, "ORDER_COMPLETE",
                    "完成交易加分", orderId, null);
            applyScore(buyerUserId, "BUYER", 3, "FIRST_TRADE",
                    "首次成功交易奖励", orderId, null);
        } else {
            applyScore(buyerUserId, "BUYER", 2, "ORDER_COMPLETE",
                    "完成交易加分", orderId, null);
        }

        // -------- 卖家加分 --------
        // 判断卖家身份：OFFICIAL_SELLER 加店铺健康分，普通用户加二手卖家分
        User seller = userMapper.selectById(sellerUserId);
        String sellerRole = isOfficialSeller(seller) ? "SELLER" : "SH_SELLER";

        int sellerDelta = 2; // 基础分

        // 该订单是否有5星好评（买家给卖家的评价）
        boolean hasFiveStar = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getOrderId, orderId)
                        .eq(Review::getUserId, buyerUserId)
                        .eq(Review::getScore, 5)) > 0;
        if (hasFiveStar) sellerDelta += 1;

        // 卖家好评率>=95% 且售出>=10单
        int soldCount = countSellerSoldOrders(sellerUserId);
        double goodRate = calcSellerGoodRate(sellerUserId, soldCount);
        if (soldCount >= 10 && goodRate >= 95.0) sellerDelta += 1;

        applyScore(sellerUserId, sellerRole, sellerDelta, "ORDER_COMPLETE",
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
        // 买家纠纷扣买家信用分（credit_score）
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
        // 判断卖家类型：OFFICIAL_SELLER 扣店铺健康分，普通用户扣二手卖家分
        User seller = userMapper.selectById(sellerUserId);
        String sellerRole = isOfficialSeller(seller) ? "SELLER" : "SH_SELLER";
        applyScore(sellerUserId, sellerRole, delta, reasonCode, desc, orderId, null);
    }

    @Override
    @Transactional
    public void onReportUpheld(Long reportId, Long reportedId, String tradeContext,
                                String reasonType, Integer customDelta) {
        int delta = (customDelta != null) ? -Math.abs(customDelta) : calcReportPenalty(reasonType);

        // 近2年被判成立>=3次，额外扣5分
        int upheldCount = countUpheldReportsIn2Years(reportedId);
        if (upheldCount >= 3) delta -= 5;

        /*
         * 根据 tradeContext 决定扣被举报人哪个信用分：
         *
         * SHOP      = 买家举报店铺卖家  → 扣被举报人的店铺健康分 (SELLER)
         * SH_BUYER  = 买家举报二手卖家  → 扣被举报人的二手卖家分 (SH_SELLER)
         * SH_SELLER = 卖家举报买家      → 扣被举报人的买家信用分 (BUYER)
         */
        String scoreRole;
        if ("SH_SELLER".equals(tradeContext)) {
            scoreRole = "BUYER";
        } else if ("SH_BUYER".equals(tradeContext)) {
            scoreRole = "SH_SELLER";
        } else {
            // SHOP：买家举报店铺卖家，扣店铺健康分
            scoreRole = "SELLER";
        }

        applyScore(reportedId, scoreRole, delta, "REPORT_UPHELD",
                "举报成立扣分：" + reasonType, reportId, null);
    }

    @Override
    @Transactional
    public void adminAdjust(Long userId, String role, int delta, String remark, Long adminId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        int current = currentScoreForRole(user, role);
        int next = current + delta;
        if (next < 0 || next > 100) {
            throw new BusinessException(400, "调整后信用分必须介于0-100之间");
        }
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

    private int currentScoreForRole(User user, String role) {
        switch (role) {
            case "SELLER":
                return user.getSellerCreditScore() == null ? 100 : user.getSellerCreditScore();
            case "SH_SELLER":
                return user.getShSellerCreditScore() == null ? 100 : user.getShSellerCreditScore();
            case "BUYER":
                return user.getCreditScore() == null ? 100 : user.getCreditScore();
            default:
                throw new BusinessException(400, "信用维度不支持");
        }
    }

    /**
     * 核心：更新user表分数 + 写日志（带上下限0~100保护）
     *
     * role 取值：
     *   BUYER     → credit_score（买家信用分，所有用户都有）
     *   SH_SELLER → buyer_credit_score（二手卖家信用分，所有用户都有；DB 列复用 buyer_credit_score）
     *   SELLER    → seller_credit_score（店铺账户健康分，仅 OFFICIAL_SELLER 有意义）
     */
    private void applyScore(Long userId, String role, int delta,
                             String reasonCode, String reasonDesc,
                             Long refId, Long operatorId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        int current;
        switch (role) {
            case "SELLER":
                current = user.getSellerCreditScore() == null ? 100 : user.getSellerCreditScore();
                break;
            case "SH_SELLER":
                current = user.getShSellerCreditScore() == null ? 100 : user.getShSellerCreditScore();
                break;
            default: // BUYER
                current = user.getCreditScore() == null ? 100 : user.getCreditScore();
                break;
        }

        int newScore = Math.min(100, Math.max(0, current + delta));
        int actualDelta = newScore - current;
        if (actualDelta == 0) return; // 边界无变化

        switch (role) {
            case "SELLER":
                user.setSellerCreditScore(newScore);
                break;
            case "SH_SELLER":
                user.setShSellerCreditScore(newScore);
                break;
            default:
                user.setCreditScore(newScore);
                break;
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
        boolean officialSeller = isOfficialSeller(user);

        // -------- 买家信用分（所有用户） --------
        int buyerScore = user.getCreditScore() == null ? 100 : user.getCreditScore();
        vo.setBuyerScore(buyerScore);
        vo.setBuyerLevel(calcLevel(buyerScore));
        int buyerOrderCount = countBuyerOrders(uid);
        vo.setBuyerOrderCount(buyerOrderCount);
        vo.setBuyerDisputeCount(countUpheldReportsIn2Years(uid));
        vo.setBuyerLogs(buildLogVOs(uid, "BUYER"));

        // -------- 二手卖家信用分（所有用户） --------
        int shSellerScore = user.getShSellerCreditScore() == null ? 100 : user.getShSellerCreditScore();
        vo.setShSellerScore(shSellerScore);
        vo.setShSellerLevel(calcLevel(shSellerScore));
        int shSellerSoldCount = countShSellerSoldOrders(uid);
        vo.setShSellerSoldCount(shSellerSoldCount);
        int shSellerGoodReview = countShSellerGoodReviews(uid);
        vo.setShSellerGoodReviewCount(shSellerGoodReview);
        vo.setShSellerDisputeCount(countUpheldReportsIn2Years(uid));
        vo.setShSellerGoodRate(shSellerSoldCount == 0 ? 100.0
                : Math.round(shSellerGoodReview * 1000.0 / shSellerSoldCount) / 10.0);
        vo.setShSellerLogs(buildLogVOs(uid, "SH_SELLER"));

        // -------- 店铺账户健康分（仅 OFFICIAL_SELLER） --------
        vo.setIsOfficialSeller(officialSeller);
        if (officialSeller) {
            int shopScore = user.getSellerCreditScore() == null ? 100 : user.getSellerCreditScore();
            vo.setShopScore(shopScore);
            vo.setShopLevel(calcLevel(shopScore));
            int shopSoldCount = countShopSoldOrders(uid);
            vo.setShopSoldCount(shopSoldCount);
            int shopGoodReview = countShopGoodReviews(uid);
            vo.setShopGoodRate(shopSoldCount == 0 ? 100.0
                    : Math.round(shopGoodReview * 1000.0 / shopSoldCount) / 10.0);
            vo.setShopLogs(buildLogVOs(uid, "SELLER"));

            // 综合评分（官方卖家）：买家30% + 二手卖家30% + 店铺40%
            int shopScore2 = user.getSellerCreditScore() == null ? 100 : user.getSellerCreditScore();
            int overall = (int) Math.round(buyerScore * 0.3 + shSellerScore * 0.3 + shopScore2 * 0.4);
            vo.setOverallScore(overall);
            vo.setOverallLevel(calcLevel(overall));
        } else {
            // 综合评分（普通用户）：买家50% + 二手卖家50%
            int overall = (int) Math.round((buyerScore + shSellerScore) / 2.0);
            vo.setOverallScore(overall);
            vo.setOverallLevel(calcLevel(overall));
        }

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

    // -------- 角色判断 --------

    private boolean isOfficialSeller(User user) {
        if (user == null || user.getRole() == null) return false;
        return "OFFICIAL_SELLER".equals(user.getRole());
    }

    // -------- 统计辅助 --------

    private int countBuyerOrders(Long userId) {
        return orderInfoMapper.selectCount(
                new QueryWrapper<OrderInfo>()
                        .eq("buyer_user_id", userId)
                        .eq("order_status", OrderStatusEnum.COMPLETED.getCode())).intValue();
    }

    /**
     * 统计用户作为二手卖家的售出订单（secondhand_product 来源）
     */
    private int countShSellerSoldOrders(Long sellerUserId) {
        List<Long> orderIds = orderItemMapper.selectList(
                        new QueryWrapper<OrderItem>()
                                .select("order_id")
                                .inSql("product_id",
                                        "SELECT id FROM secondhand_product WHERE seller_user_id=" + sellerUserId))
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

    /**
     * 统计用户作为店铺卖家的售出订单（product 来源，仅 OFFICIAL_SELLER）
     */
    private int countShopSoldOrders(Long sellerUserId) {
        List<Long> orderIds = orderItemMapper.selectList(
                        new QueryWrapper<OrderItem>()
                                .select("order_id")
                                .inSql("product_id",
                                        "SELECT id FROM product WHERE shop_id IN " +
                                        "(SELECT id FROM shop WHERE owner_user_id=" + sellerUserId + ")"))
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

    /**
     * 统计卖家在二手交易中收到的好评（score >= 4）
     */
    private int countShSellerGoodReviews(Long sellerUserId) {
        List<Long> orderIds = orderItemMapper.selectList(
                        new QueryWrapper<OrderItem>()
                                .select("order_id")
                                .inSql("product_id",
                                        "SELECT id FROM secondhand_product WHERE seller_user_id=" + sellerUserId))
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

    /**
     * 统计店铺卖家在店铺交易中收到的好评（仅 OFFICIAL_SELLER）
     */
    private int countShopGoodReviews(Long sellerUserId) {
        List<Long> orderIds = orderItemMapper.selectList(
                        new QueryWrapper<OrderItem>()
                                .select("order_id")
                                .inSql("product_id",
                                        "SELECT id FROM product WHERE shop_id IN " +
                                        "(SELECT id FROM shop WHERE owner_user_id=" + sellerUserId + ")"))
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

    private int countSellerSoldOrders(Long sellerUserId) {
        // 兼容旧调用：返回二手+店铺合计售出数（用于好评率加分判断）
        return countShSellerSoldOrders(sellerUserId) + countShopSoldOrders(sellerUserId);
    }

    private double calcSellerGoodRate(Long sellerUserId, int soldCount) {
        if (soldCount == 0) return 100.0;
        int good = countShSellerGoodReviews(sellerUserId) + countShopGoodReviews(sellerUserId);
        return Math.round(good * 1000.0 / soldCount) / 10.0;
    }

    private int countUpheldReportsIn2Years(Long userId) {
        return userReportMapper.countUpheldReportsIn2Years(userId, LocalDateTime.now().minusYears(2));
    }
}

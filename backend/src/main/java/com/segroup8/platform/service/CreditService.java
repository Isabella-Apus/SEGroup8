package com.segroup8.platform.service;

import com.segroup8.platform.vo.CreditScoreVO;

public interface CreditService {

    /**
     * 获取指定用户的完整信用信息
     * 包含：买家信用分、二手卖家信用分、店铺账户健康分（OFFICIAL_SELLER）、综合评分
     */
    CreditScoreVO getCreditInfo(Long userId);

    /**
     * 获取当前登录用户自己的信用信息
     */
    CreditScoreVO getMyCredit();

    /**
     * 订单完成时触发信用分更新
     * -----------------------------------------------
     * 【买家加分规则】（扣 credit_score）
     *   正常完成交易：+2分
     *   首次成功交易（历史首单）：额外+3分
     *   上限保护：总分上限100
     *
     * 【卖家加分规则】
     *   OFFICIAL_SELLER → 加 seller_credit_score（店铺账户健康分）
     *   普通用户卖二手  → 加 sh_seller_credit_score（二手卖家信用分）
     *   正常完成交易：+2分
     *   收到5星好评：额外+1分
     *   好评率 >= 95%（且售出>=10单）：额外+1分
     *
     * 同一订单只触发一次（幂等处理）
     *
     * @param orderId      订单ID
     * @param buyerUserId  买家用户ID
     * @param sellerUserId 卖家用户ID
     */
    void onOrderComplete(Long orderId, Long buyerUserId, Long sellerUserId);

    /**
     * 订单发生纠纷且判定为买家责任时扣买家信用分（credit_score）
     * -----------------------------------------------
     * 恶意退款：-10分
     * 频繁取消订单：-5分
     * 普通纠纷判责：-5分
     *
     * @param orderId     订单ID
     * @param buyerUserId 买家用户ID
     * @param reasonCode  REFUND_ABUSE / CANCEL_OFTEN / ORDER_DISPUTE
     */
    void onBuyerDisputePenalty(Long orderId, Long buyerUserId, String reasonCode);

    /**
     * 订单发生纠纷且判定为卖家责任时扣分
     * -----------------------------------------------
     * OFFICIAL_SELLER → 扣 seller_credit_score（店铺账户健康分）
     * 普通用户卖二手  → 扣 sh_seller_credit_score（二手卖家信用分）
     *
     * 商品描述不符：-8分
     * 普通纠纷判责：-5分
     *
     * @param orderId      订单ID
     * @param sellerUserId 卖家用户ID
     * @param reasonCode   FAKE_ITEM / ORDER_DISPUTE
     */
    void onSellerDisputePenalty(Long orderId, Long sellerUserId, String reasonCode);

    /**
     * 举报成立后扣分
     * -----------------------------------------------
     * 扣分规则由 tradeContext 决定扣哪个字段：
     *   SH_BUYER  = 买家举报二手卖家   → 扣被举报人 sh_seller_credit_score
     *   SH_SELLER = 卖家举报二手买家   → 扣被举报人 credit_score（买家信用）
     *   SHOP      = 买家举报店铺卖家   →
     *     被举报人是 OFFICIAL_SELLER  → 扣 seller_credit_score（店铺健康分）
     *     被举报人不是 OFFICIAL_SELLER → 扣 sh_seller_credit_score（二手卖家分）
     *
     * 举报类型扣分：
     *   FRAUD -20 / REFUND_ABUSE -10 / FAKE_ITEM -8 / SPAM -6 / BAD_ATTITUDE -5 / OTHER -3
     *
     * 累计被判成立>=3次（近2年）：额外再扣5分
     *
     * @param reportId     举报记录ID
     * @param reportedId   被举报用户ID
     * @param tradeContext 交易场景：SHOP / SH_BUYER / SH_SELLER
     * @param reasonType   举报类型
     * @param customDelta  管理员自定义扣分，null则按规则自动计算
     */
    void onReportUpheld(Long reportId, Long reportedId, String tradeContext,
                        String reasonType, Integer customDelta);

    /**
     * 管理员手动调整信用分
     *
     * @param userId  目标用户ID
     * @param role    调整的信用维度：BUYER / SH_SELLER / SELLER
     * @param delta   调整值，正为加分，负为扣分
     * @param remark  调整原因说明
     * @param adminId 操作管理员ID
     */
    void adminAdjust(Long userId, String role, int delta, String remark, Long adminId);

    /**
     * 根据分数计算信用等级
     *   0  ~ 49  : 较差
     *   50 ~ 69  : 良好
     *   70 ~ 89  : 优秀
     *   90 ~ 100 : 极好
     */
    String calcLevel(int score);
}

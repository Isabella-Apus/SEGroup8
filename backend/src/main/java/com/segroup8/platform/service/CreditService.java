package com.segroup8.platform.service;

import com.segroup8.platform.vo.CreditScoreVO;

public interface CreditService {

    /**
     * 获取指定用户的完整信用信息（买家+卖家）
     * 包含分数、等级、统计数据、最近变动记录
     */
    CreditScoreVO getCreditInfo(Long userId);

    /**
     * 获取当前登录用户自己的信用信息
     */
    CreditScoreVO getMyCredit();

    /**
     * 订单完成时触发信用分更新
     * -----------------------------------------------
     * 【买家加分规则】
     *   正常完成交易：+2分
     *   首次成功交易（历史首单）：额外+3分
     *   上限保护：单笔订单买家最多加5分，总分上限100
     *
     * 【卖家加分规则】
     *   正常完成交易：+2分
     *   收到5星好评：额外+1分
     *   好评率 >= 95%（且售出>=10单）：额外+1分
     *   上限保护：单笔订单卖家最多加4分，总分上限100
     *
     * 同一订单只触发一次，重复调用幂等处理
     *
     * @param orderId     订单ID
     * @param buyerUserId 买家用户ID
     * @param sellerUserId 卖家用户ID（商家卖家）
     */
    void onOrderComplete(Long orderId, Long buyerUserId, Long sellerUserId);

    /**
     * 订单发生纠纷且判定为买家责任时扣买家分
     * -----------------------------------------------
     * 【扣分规则】
     *   恶意退款：-10分
     *   频繁取消订单（30天内>=3次）：-5分
     *   普通纠纷判责：-5分
     *   下限保护：总分最低为0
     *
     * @param orderId     订单ID
     * @param buyerUserId 买家用户ID
     * @param reasonCode  原因码：REFUND_ABUSE / CANCEL_OFTEN / ORDER_DISPUTE
     */
    void onBuyerDisputePenalty(Long orderId, Long buyerUserId, String reasonCode);

    /**
     * 订单发生纠纷且判定为卖家责任时扣卖家分
     * -----------------------------------------------
     * 【扣分规则】
     *   商品描述不符：-8分
     *   拒绝合理退款：-6分
     *   普通纠纷判责：-5分
     *   下限保护：总分最低为0
     *
     * @param orderId      订单ID
     * @param sellerUserId 卖家用户ID
     * @param reasonCode   原因码：FAKE_ITEM / ORDER_DISPUTE
     */
    void onSellerDisputePenalty(Long orderId, Long sellerUserId, String reasonCode);

    /**
     * 举报成立后扣分
     * -----------------------------------------------
     * 【扣分规则（被举报方）】
     *   FRAUD          诈骗：-20分
     *   FAKE_ITEM      商品不符：-8分
     *   BAD_ATTITUDE   态度恶劣：-5分
     *   REFUND_ABUSE   恶意退款：-10分
     *   SPAM           刷单骚扰：-6分
     *   OTHER          其他：-3分
     *
     *   累计被判成立举报>=3次（近2年）：额外再扣5分
     *   下限保护：总分最低为0
     *
     * @param reportId     举报记录ID
     * @param reportedId   被举报用户ID
     * @param reportedRole 被举报用户的身份：BUYER / SELLER
     * @param reasonType   举报类型
     * @param customDelta  管理员自定义扣分，null则按上述规则自动计算
     */
    void onReportUpheld(Long reportId, Long reportedId, String reportedRole,
                        String reasonType, Integer customDelta);

    /**
     * 管理员手动调整信用分
     *
     * @param userId      目标用户ID
     * @param role        调整的身份：BUYER / SELLER
     * @param delta       调整值，正为加分，负为扣分
     * @param remark      调整原因说明
     * @param adminId     操作管理员ID
     */
    void adminAdjust(Long userId, String role, int delta, String remark, Long adminId);

    /**
     * 根据分数计算信用等级
     * -----------------------------------------------
     * 分数区间（参考闲鱼）：
     *   0  ~ 49  : 较差
     *   50 ~ 69  : 良好
     *   70 ~ 89  : 优秀
     *   90 ~ 100 : 极好
     */
    String calcLevel(int score);
}
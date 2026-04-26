package com.segroup8.platform.vo;

import java.util.List;

/**
 * 用户信用信息 VO
 *
 * 所有用户都有：
 *   - buyerScore     买家信用分（credit_score 字段）
 *   - shSellerScore  二手卖家信用分（buyer_credit_score DB 列）
 *
 * 仅 OFFICIAL_SELLER 才显示：
 *   - shopScore      店铺账户健康分（seller_credit_score 字段）
 *
 * 综合评分 = 买家信用分和二手卖家信用分的加权平均（店铺健康分作为参考折算进综合评分，不单独展示）
 */
public class CreditScoreVO {

    // -------- 买家信用 --------

    /** 买家信用分（满分100，最低0） */
    private Integer buyerScore;

    /** 买家信用等级：较差 / 良好 / 优秀 / 极好 */
    private String buyerLevel;

    /** 买家成功交易次数 */
    private Integer buyerOrderCount;

    /** 买家近2年被判定成立的纠纷/举报数 */
    private Integer buyerDisputeCount;

    /** 最近10条买家信用分变动记录 */
    private List<CreditLogItemVO> buyerLogs;

    // -------- 二手卖家信用 --------

    /** 二手卖家信用分（满分100，最低0） */
    private Integer shSellerScore;

    /** 二手卖家信用等级：较差 / 良好 / 优秀 / 极好 */
    private String shSellerLevel;

    /** 二手卖家累计售出订单数 */
    private Integer shSellerSoldCount;

    /** 二手卖家好评率（0~100，保留1位小数） */
    private Double shSellerGoodRate;

    /** 二手卖家收到的好评数 */
    private Integer shSellerGoodReviewCount;

    /** 二手卖家近2年被判定成立的纠纷/举报数 */
    private Integer shSellerDisputeCount;

    /** 最近10条二手卖家变动记录 */
    private List<CreditLogItemVO> shSellerLogs;

    // -------- 店铺账户健康（仅 OFFICIAL_SELLER 有效） --------

    /** 是否是官方入驻卖家（true 时 shopScore 才有意义） */
    private Boolean isOfficialSeller;

    /** 店铺账户健康分（满分100，最低0；已折算进综合评分，不单独弹窗） */
    private Integer shopScore;

    /** 店铺账户健康等级 */
    private String shopLevel;

    /** 店铺累计售出订单数 */
    private Integer shopSoldCount;

    /** 店铺好评率 */
    private Double shopGoodRate;

    /** 最近10条店铺健康分变动记录 */
    private List<CreditLogItemVO> shopLogs;

    // -------- 综合评分 --------

    /**
     * 综合信用评分（前端展示主要入口）
     *
     * 计算规则：
     *   - 普通用户：综合 = (buyerScore + shSellerScore) / 2
     *   - 官方卖家：综合 = buyerScore * 0.3 + shSellerScore * 0.3 + shopScore * 0.4
     */
    private Integer overallScore;

    /** 综合信用等级 */
    private String overallLevel;

    // -------- 内部类：单条变动记录 --------

    public static class CreditLogItemVO {
        /** 变动分值，正为加分，负为扣分 */
        private Integer delta;
        /** 原因描述（已转为中文） */
        private String reasonDesc;
        /** 变动时间，格式：yyyy-MM-dd HH:mm */
        private String createTime;

        public Integer getDelta() { return delta; }
        public void setDelta(Integer delta) { this.delta = delta; }

        public String getReasonDesc() { return reasonDesc; }
        public void setReasonDesc(String reasonDesc) { this.reasonDesc = reasonDesc; }

        public String getCreateTime() { return createTime; }
        public void setCreateTime(String createTime) { this.createTime = createTime; }
    }

    // -------- getters & setters --------

    public Integer getBuyerScore() { return buyerScore; }
    public void setBuyerScore(Integer buyerScore) { this.buyerScore = buyerScore; }

    public String getBuyerLevel() { return buyerLevel; }
    public void setBuyerLevel(String buyerLevel) { this.buyerLevel = buyerLevel; }

    public Integer getBuyerOrderCount() { return buyerOrderCount; }
    public void setBuyerOrderCount(Integer buyerOrderCount) { this.buyerOrderCount = buyerOrderCount; }

    public Integer getBuyerDisputeCount() { return buyerDisputeCount; }
    public void setBuyerDisputeCount(Integer buyerDisputeCount) { this.buyerDisputeCount = buyerDisputeCount; }

    public List<CreditLogItemVO> getBuyerLogs() { return buyerLogs; }
    public void setBuyerLogs(List<CreditLogItemVO> buyerLogs) { this.buyerLogs = buyerLogs; }

    public Integer getShSellerScore() { return shSellerScore; }
    public void setShSellerScore(Integer shSellerScore) { this.shSellerScore = shSellerScore; }

    public String getShSellerLevel() { return shSellerLevel; }
    public void setShSellerLevel(String shSellerLevel) { this.shSellerLevel = shSellerLevel; }

    public Integer getShSellerSoldCount() { return shSellerSoldCount; }
    public void setShSellerSoldCount(Integer shSellerSoldCount) { this.shSellerSoldCount = shSellerSoldCount; }

    public Double getShSellerGoodRate() { return shSellerGoodRate; }
    public void setShSellerGoodRate(Double shSellerGoodRate) { this.shSellerGoodRate = shSellerGoodRate; }

    public Integer getShSellerGoodReviewCount() { return shSellerGoodReviewCount; }
    public void setShSellerGoodReviewCount(Integer shSellerGoodReviewCount) { this.shSellerGoodReviewCount = shSellerGoodReviewCount; }

    public Integer getShSellerDisputeCount() { return shSellerDisputeCount; }
    public void setShSellerDisputeCount(Integer shSellerDisputeCount) { this.shSellerDisputeCount = shSellerDisputeCount; }

    public List<CreditLogItemVO> getShSellerLogs() { return shSellerLogs; }
    public void setShSellerLogs(List<CreditLogItemVO> shSellerLogs) { this.shSellerLogs = shSellerLogs; }

    public Boolean getIsOfficialSeller() { return isOfficialSeller; }
    public void setIsOfficialSeller(Boolean isOfficialSeller) { this.isOfficialSeller = isOfficialSeller; }

    public Integer getShopScore() { return shopScore; }
    public void setShopScore(Integer shopScore) { this.shopScore = shopScore; }

    public String getShopLevel() { return shopLevel; }
    public void setShopLevel(String shopLevel) { this.shopLevel = shopLevel; }

    public Integer getShopSoldCount() { return shopSoldCount; }
    public void setShopSoldCount(Integer shopSoldCount) { this.shopSoldCount = shopSoldCount; }

    public Double getShopGoodRate() { return shopGoodRate; }
    public void setShopGoodRate(Double shopGoodRate) { this.shopGoodRate = shopGoodRate; }

    public List<CreditLogItemVO> getShopLogs() { return shopLogs; }
    public void setShopLogs(List<CreditLogItemVO> shopLogs) { this.shopLogs = shopLogs; }

    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }

    public String getOverallLevel() { return overallLevel; }
    public void setOverallLevel(String overallLevel) { this.overallLevel = overallLevel; }
}
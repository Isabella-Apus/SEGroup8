package com.segroup8.platform.vo;

import java.util.List;

public class CreditScoreVO {

    // -------- 买家信用 --------

    /** 买家信用分（满分100，最低0） */
    private Integer buyerScore;

    /** 买家信用等级：较差 / 良好 / 优秀 / 极好 */
    private String buyerLevel;

    /** 买家成功交易次数 */
    private Integer buyerOrderCount;

    /** 买家收到的好评数 */
    private Integer buyerGoodReviewCount;

    /** 买家近2年被判定成立的纠纷数 */
    private Integer buyerDisputeCount;

    /** 买家好评率（0~100，保留1位小数） */
    private Double buyerGoodRate;

    // -------- 卖家信用 --------

    /** 卖家信用分（满分100，最低0） */
    private Integer sellerScore;

    /** 卖家信用等级：较差 / 良好 / 优秀 / 极好 */
    private String sellerLevel;

    /** 卖家累计售出订单数（不含全额退款/关闭） */
    private Integer sellerSoldCount;

    /** 卖家收到的好评数 */
    private Integer sellerGoodReviewCount;

    /** 卖家近2年被判定成立的纠纷数 */
    private Integer sellerDisputeCount;

    /** 卖家好评率（0~100，保留1位小数） */
    private Double sellerGoodRate;

    // -------- 通用 --------

    /** 最近10条买家信用分变动记录 */
    private List<CreditLogItemVO> buyerLogs;

    /** 最近10条卖家信用分变动记录 */
    private List<CreditLogItemVO> sellerLogs;

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

    public Integer getBuyerGoodReviewCount() { return buyerGoodReviewCount; }
    public void setBuyerGoodReviewCount(Integer buyerGoodReviewCount) { this.buyerGoodReviewCount = buyerGoodReviewCount; }

    public Integer getBuyerDisputeCount() { return buyerDisputeCount; }
    public void setBuyerDisputeCount(Integer buyerDisputeCount) { this.buyerDisputeCount = buyerDisputeCount; }

    public Double getBuyerGoodRate() { return buyerGoodRate; }
    public void setBuyerGoodRate(Double buyerGoodRate) { this.buyerGoodRate = buyerGoodRate; }

    public Integer getSellerScore() { return sellerScore; }
    public void setSellerScore(Integer sellerScore) { this.sellerScore = sellerScore; }

    public String getSellerLevel() { return sellerLevel; }
    public void setSellerLevel(String sellerLevel) { this.sellerLevel = sellerLevel; }

    public Integer getSellerSoldCount() { return sellerSoldCount; }
    public void setSellerSoldCount(Integer sellerSoldCount) { this.sellerSoldCount = sellerSoldCount; }

    public Integer getSellerGoodReviewCount() { return sellerGoodReviewCount; }
    public void setSellerGoodReviewCount(Integer sellerGoodReviewCount) { this.sellerGoodReviewCount = sellerGoodReviewCount; }

    public Integer getSellerDisputeCount() { return sellerDisputeCount; }
    public void setSellerDisputeCount(Integer sellerDisputeCount) { this.sellerDisputeCount = sellerDisputeCount; }

    public Double getSellerGoodRate() { return sellerGoodRate; }
    public void setSellerGoodRate(Double sellerGoodRate) { this.sellerGoodRate = sellerGoodRate; }

    public List<CreditLogItemVO> getBuyerLogs() { return buyerLogs; }
    public void setBuyerLogs(List<CreditLogItemVO> buyerLogs) { this.buyerLogs = buyerLogs; }

    public List<CreditLogItemVO> getSellerLogs() { return sellerLogs; }
    public void setSellerLogs(List<CreditLogItemVO> sellerLogs) { this.sellerLogs = sellerLogs; }
}
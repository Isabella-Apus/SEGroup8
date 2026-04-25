package com.segroup8.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserReportRequest {

    /**
     * 被举报用户ID
     */
    @NotNull(message = "被举报用户ID不能为空")
    private Long reportedId;

    /**
     * 举报类型，枚举值：
     * FRAUD          诈骗/虚假交易
     * FAKE_ITEM      商品与描述不符
     * BAD_ATTITUDE   态度恶劣/骚扰
     * REFUND_ABUSE   恶意退款
     * SPAM           刷单/广告骚扰
     * OTHER          其他
     */
    @NotBlank(message = "举报类型不能为空")
    private String reasonType;

    /**
     * 补充说明（可选，最多500字）
     */
    private String reasonDesc;

    /**
     * 证据图片URL，逗号分隔（可选）
     * 例："http://xxx/a.jpg,http://xxx/b.jpg"
     */
    private String evidenceUrls;

    /**
     * 交易场景，决定扣哪种信用分：
     * SHOP      = 从全新商品店铺购买，买家举报卖家 → 扣卖家店铺账户健康（seller_credit_score）
     * SH_BUYER  = 二手交易，买家举报卖家 → 扣被举报人的二手卖家分（buyer_credit_score）
     * SH_SELLER = 二手交易，卖家举报买家 → 扣被举报人的买家信用分（credit_score）
     * 不传时默认 SHOP
     */
    private String tradeContext = "SHOP";

    public String getTradeContext() { return tradeContext; }
    public void setTradeContext(String tradeContext) { this.tradeContext = tradeContext; }

    /**
     * 举报人当时的身份，由后端根据登录用户角色自动判断，
     * 前端无需传入，此字段仅供内部流转使用
     */
    private String reporterRole;

    // ---------- getters & setters ----------

    public Long getReportedId() { return reportedId; }
    public void setReportedId(Long reportedId) { this.reportedId = reportedId; }

    public String getReasonType() { return reasonType; }
    public void setReasonType(String reasonType) { this.reasonType = reasonType; }

    public String getReasonDesc() { return reasonDesc; }
    public void setReasonDesc(String reasonDesc) { this.reasonDesc = reasonDesc; }

    public String getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(String evidenceUrls) { this.evidenceUrls = evidenceUrls; }

    public String getReporterRole() { return reporterRole; }
    public void setReporterRole(String reporterRole) { this.reporterRole = reporterRole; }
}
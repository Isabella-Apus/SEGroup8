package com.segroup8.platform.dto;

import jakarta.validation.constraints.NotNull;

public class AdminReportAuditRequest {

    /**
     * 举报记录ID
     */
    @NotNull(message = "举报ID不能为空")
    private Long reportId;

    /**
     * 审核结果：
     * 1 = 成立（扣分）
     * 2 = 不成立（驳回）
     */
    @NotNull(message = "审核结果不能为空")
    private Integer decision;

    /**
     * 管理员备注（可选，建议驳回时填写原因）
     */
    private String adminRemark;

    /**
     * 成立时手动指定扣分值（可选）
     * 若不填则由系统按举报类型自动计算扣分
     * 范围建议：1 ~ 30
     */
    private Integer customDelta;

    // ---------- getters & setters ----------

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public Integer getDecision() { return decision; }
    public void setDecision(Integer decision) { this.decision = decision; }

    public String getAdminRemark() { return adminRemark; }
    public void setAdminRemark(String adminRemark) { this.adminRemark = adminRemark; }

    public Integer getCustomDelta() { return customDelta; }
    public void setCustomDelta(Integer customDelta) { this.customDelta = customDelta; }
}
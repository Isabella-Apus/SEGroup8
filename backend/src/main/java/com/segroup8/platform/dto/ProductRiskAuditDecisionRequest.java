package com.segroup8.platform.dto;

import jakarta.validation.constraints.NotBlank;

public class ProductRiskAuditDecisionRequest {

    @NotBlank(message = "decision is required")
    private String decision;
    private String adminRemark;

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getAdminRemark() {
        return adminRemark;
    }

    public void setAdminRemark(String adminRemark) {
        this.adminRemark = adminRemark;
    }
}

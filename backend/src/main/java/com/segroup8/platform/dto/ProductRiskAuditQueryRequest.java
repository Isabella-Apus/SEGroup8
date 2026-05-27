package com.segroup8.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class ProductRiskAuditQueryRequest {

    @Min(value = 1, message = "pageNum min is 1")
    private Long pageNum = 1L;

    @Min(value = 1, message = "pageSize min is 1")
    @Max(value = 100, message = "pageSize max is 100")
    private Long pageSize = 10L;

    private String productType;
    private String riskLevel;
    private String auditStatus;
    private Long sellerUserId;
    private String keyword;

    public Long getPageNum() {
        return pageNum;
    }

    public void setPageNum(Long pageNum) {
        this.pageNum = pageNum;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(String auditStatus) {
        this.auditStatus = auditStatus;
    }

    public Long getSellerUserId() {
        return sellerUserId;
    }

    public void setSellerUserId(Long sellerUserId) {
        this.sellerUserId = sellerUserId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}

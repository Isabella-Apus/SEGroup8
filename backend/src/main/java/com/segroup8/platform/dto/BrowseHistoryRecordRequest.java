package com.segroup8.platform.dto;

import jakarta.validation.constraints.NotNull;

public class BrowseHistoryRecordRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    private String productType;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }
}

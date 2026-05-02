package com.segroup8.platform.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BargainApplyRequest {

    @NotNull(message = "二手商品ID不能为空")
    private Long productId;

    @NotNull(message = "卖家ID不能为空")
    private Long sellerUserId;

    @NotNull(message = "议价金额不能为空")
    @DecimalMin(value = "0.01", message = "议价金额必须大于0")
    private BigDecimal proposedPrice;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getSellerUserId() {
        return sellerUserId;
    }

    public void setSellerUserId(Long sellerUserId) {
        this.sellerUserId = sellerUserId;
    }

    public BigDecimal getProposedPrice() {
        return proposedPrice;
    }

    public void setProposedPrice(BigDecimal proposedPrice) {
        this.proposedPrice = proposedPrice;
    }
}

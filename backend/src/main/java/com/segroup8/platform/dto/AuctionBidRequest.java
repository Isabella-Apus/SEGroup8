package com.segroup8.platform.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AuctionBidRequest {

    @NotNull(message = "出价金额不能为空")
    @DecimalMin(value = "0.01", message = "出价金额必须大于0")
    private BigDecimal bidAmount;

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(BigDecimal bidAmount) {
        this.bidAmount = bidAmount;
    }
}

package com.segroup8.platform.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BargainConfirmRequest {

    @NotNull(message = "议价记录ID不能为空")
    private Long negotiationId;

    @NotNull(message = "确认价格不能为空")
    @DecimalMin(value = "0.01", message = "确认价格必须大于0")
    private BigDecimal confirmedPrice;

    public Long getNegotiationId() {
        return negotiationId;
    }

    public void setNegotiationId(Long negotiationId) {
        this.negotiationId = negotiationId;
    }

    public BigDecimal getConfirmedPrice() {
        return confirmedPrice;
    }

    public void setConfirmedPrice(BigDecimal confirmedPrice) {
        this.confirmedPrice = confirmedPrice;
    }
}

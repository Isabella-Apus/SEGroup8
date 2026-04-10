package com.segroup8.platform.dto;

import jakarta.validation.constraints.NotNull;

public class LogisticsPushNextRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}

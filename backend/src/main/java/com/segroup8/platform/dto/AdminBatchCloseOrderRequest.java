package com.segroup8.platform.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AdminBatchCloseOrderRequest {

    @NotEmpty(message = "订单ID列表不能为空")
    private List<@NotNull(message = "订单ID不能为空") Long> orderIds;

    public List<Long> getOrderIds() {
        return orderIds;
    }

    public void setOrderIds(List<Long> orderIds) {
        this.orderIds = orderIds;
    }
}


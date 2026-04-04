package com.segroup8.platform.dto;

import jakarta.validation.constraints.NotNull;

public class ProductStockAdjustRequest {

    @NotNull(message = "库存变更值不能为空")
    private Integer delta;

    public Integer getDelta() {
        return delta;
    }

    public void setDelta(Integer delta) {
        this.delta = delta;
    }
}

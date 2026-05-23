package com.segroup8.platform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateOrderRequest {

    @NotEmpty(message = "订单商品不能为空")
    @Valid
    private List<CreateOrderItemRequest> items;

    private Long addressId;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    public List<CreateOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CreateOrderItemRequest> items) {
        this.items = items;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }
}

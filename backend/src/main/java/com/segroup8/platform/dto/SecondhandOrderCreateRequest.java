package com.segroup8.platform.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public class SecondhandOrderCreateRequest {

    @NotNull(message = "请选择收货地址")
    private Long addressId;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

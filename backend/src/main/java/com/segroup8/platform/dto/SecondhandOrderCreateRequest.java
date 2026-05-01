package com.segroup8.platform.dto;

import jakarta.validation.constraints.Size;

public class SecondhandOrderCreateRequest {

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


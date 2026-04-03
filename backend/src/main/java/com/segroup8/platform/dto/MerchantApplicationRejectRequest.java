package com.segroup8.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MerchantApplicationRejectRequest {

    @NotBlank(message = "驳回理由不能为空")
    @Size(max = 255, message = "驳回理由长度不能超过255")
    private String rejectReason;

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}

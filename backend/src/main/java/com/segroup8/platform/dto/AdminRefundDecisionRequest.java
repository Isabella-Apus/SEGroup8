package com.segroup8.platform.dto;

import jakarta.validation.constraints.Size;

public class AdminRefundDecisionRequest {

    // 审核意见可选；为了更像淘宝后台，这里允许管理员补充一句说明
    @Size(max = 255, message = "审核意见最多255字")
    private String remark;

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}


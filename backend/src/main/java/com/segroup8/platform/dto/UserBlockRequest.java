package com.segroup8.platform.dto;

import jakarta.validation.constraints.NotNull;

public class UserBlockRequest {

    /**
     * 要拉黑/取消拉黑的用户ID
     */
    @NotNull(message = "目标用户ID不能为空")
    private Long targetUserId;

    // ---------- getters & setters ----------

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
}
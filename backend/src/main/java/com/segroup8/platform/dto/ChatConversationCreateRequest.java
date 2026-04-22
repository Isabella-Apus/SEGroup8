package com.segroup8.platform.dto;

import jakarta.validation.constraints.NotNull;

public class ChatConversationCreateRequest {

    @NotNull(message = "目标用户不能为空")
    private Long targetUserId;

    private String sourceType;
    private Long sourceId;

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }
}

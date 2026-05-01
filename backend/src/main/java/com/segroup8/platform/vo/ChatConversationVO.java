package com.segroup8.platform.vo;

import java.time.LocalDateTime;

public class ChatConversationVO {

    private Long id;
    private String sourceType;
    private Long sourceId;
    private String sourceTitle;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private ChatParticipantVO self;
    private ChatParticipantVO other;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSourceTitle() {
        return sourceTitle;
    }

    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    public String getLastMessageContent() {
        return lastMessageContent;
    }

    public void setLastMessageContent(String lastMessageContent) {
        this.lastMessageContent = lastMessageContent;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public ChatParticipantVO getSelf() {
        return self;
    }

    public void setSelf(ChatParticipantVO self) {
        this.self = self;
    }

    public ChatParticipantVO getOther() {
        return other;
    }

    public void setOther(ChatParticipantVO other) {
        this.other = other;
    }
}

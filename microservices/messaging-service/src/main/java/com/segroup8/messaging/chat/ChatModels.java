package com.segroup8.messaging.chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class ChatModels {
    private ChatModels() {}

    public record CreateConversationRequest(
            @NotNull(message = "targetUserId is required") Long targetUserId,
            String sourceType,
            Long sourceId,
            @Size(max = 120, message = "sourceTitle must not exceed 120 characters") String sourceTitle) {}
    public record SendMessageRequest(
            @Size(max = 1000, message = "content must not exceed 1000 characters") String content) {}
    public record Participant(long userId, String nickname, String avatar, String role) {}
    public record Conversation(long id, String sourceType, long sourceId, String sourceTitle,
            String lastMessageContent, LocalDateTime lastMessageTime, int unreadCount,
            Participant self, Participant other) {}
    public record Message(long id, long conversationId, long senderUserId, long receiverUserId,
            String content, int isRead, LocalDateTime createTime, Participant sender) {}
}

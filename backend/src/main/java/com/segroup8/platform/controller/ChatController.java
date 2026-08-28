package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.ChatConversationCreateRequest;
import com.segroup8.platform.dto.ChatMessageSendRequest;
import com.segroup8.platform.realtime.RealtimePushService;
import com.segroup8.platform.service.ChatService;
import com.segroup8.platform.vo.ChatConversationVO;
import com.segroup8.platform.vo.ChatMessageVO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final RealtimePushService realtimePushService;

    public ChatController(ChatService chatService, RealtimePushService realtimePushService) {
        this.chatService = chatService;
        this.realtimePushService = realtimePushService;
    }

    @GetMapping("/conversations")
    public Result<List<ChatConversationVO>> listConversations() {
        return Result.success(chatService.listMyConversations(UserContext.getUserId()));
    }

    @PostMapping("/conversations")
    public Result<ChatConversationVO> createConversation(@Valid @RequestBody ChatConversationCreateRequest request) {
        return Result.success(chatService.createOrGetConversation(
                UserContext.getUserId(),
                request.getTargetUserId(),
                request.getSourceType(),
                request.getSourceId()));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public Result<List<ChatMessageVO>> listMessages(@PathVariable Long conversationId) {
        return Result.success(chatService.listConversationMessages(UserContext.getUserId(), conversationId));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public Result<ChatMessageVO> sendMessage(@PathVariable Long conversationId,
            @Valid @RequestBody ChatMessageSendRequest request) {
        ChatMessageVO message = chatService.sendMessage(UserContext.getUserId(), conversationId, request.getContent());
        try {
            realtimePushService.pushToUsers(
                    List.of(message.getSenderUserId(), message.getReceiverUserId()),
                    "CHAT_MESSAGE",
                    message);
        } catch (RuntimeException exception) {
            log.warn("Chat message {} persisted but realtime delivery failed: {}",
                    message.getId(), exception.getMessage());
        }
        return Result.success(message);
    }
}

package com.segroup8.messaging.chat;

import com.segroup8.messaging.chat.ChatModels.CreateConversationRequest;
import com.segroup8.messaging.chat.ChatModels.SendMessageRequest;
import com.segroup8.messaging.common.ApiResult;
import com.segroup8.messaging.security.JwtAuthenticationInterceptor;
import com.segroup8.messaging.security.MessagingPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService service;
    public ChatController(ChatService service) { this.service = service; }
    @GetMapping("/conversations") public ApiResult<List<ChatModels.Conversation>> conversations(HttpServletRequest req) {
        return ApiResult.success(service.list(principal(req).userId()));
    }
    @PostMapping("/conversations") public ApiResult<ChatModels.Conversation> create(
            @Valid @RequestBody CreateConversationRequest body, HttpServletRequest req) {
        MessagingPrincipal p = principal(req); return ApiResult.success(service.createOrGet(p.userId(), body));
    }
    @GetMapping("/conversations/{id}/messages") public ApiResult<List<ChatModels.Message>> messages(
            @PathVariable long id, HttpServletRequest req) {
        return ApiResult.success(service.messages(principal(req).userId(), id));
    }
    @PostMapping("/conversations/{id}/messages") public ApiResult<ChatModels.Message> send(
            @PathVariable long id, @Valid @RequestBody SendMessageRequest body, HttpServletRequest req) {
        MessagingPrincipal p = principal(req); return ApiResult.success(service.send(p.userId(), id, body.content()));
    }
    private MessagingPrincipal principal(HttpServletRequest request) {
        return (MessagingPrincipal) request.getAttribute(JwtAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE);
    }
}

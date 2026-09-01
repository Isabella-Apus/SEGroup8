package com.segroup8.platform.controller;

import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.realtime.RealtimePushService;
import com.segroup8.platform.service.ChatService;
import com.segroup8.platform.vo.ChatConversationVO;
import com.segroup8.platform.vo.ChatMessageVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("DOMAIN_E")
@Tag("UC24")
@ExtendWith(MockitoExtension.class)
class ChatControllerUc24WebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private ChatService chatService;

    @Mock
    private RealtimePushService realtimePushService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(chatService, realtimePushService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
        UserContext.setUserId(2401L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void currentUserCreatesAndListsTheSameConversation() throws Exception {
        ChatConversationVO conversation = conversation(24L);
        when(chatService.createOrGetConversation(2401L, 2402L, "PRODUCT", 2401L))
                .thenReturn(conversation);
        when(chatService.listMyConversations(2401L)).thenReturn(List.of(conversation));

        mockMvc.perform(post("/api/chat/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":2402,\"sourceType\":\"PRODUCT\",\"sourceId\":2401}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(24));

        mockMvc.perform(get("/api/chat/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(24));
    }

    @Test
    void participantReadsAndSendsMessageWithRealtimeDelivery() throws Exception {
        ChatMessageVO message = message(101L, "buyer to seller");
        when(chatService.listConversationMessages(2401L, 24L)).thenReturn(List.of(message));
        when(chatService.sendMessage(2401L, 24L, "buyer to seller")).thenReturn(message);

        mockMvc.perform(get("/api/chat/conversations/24/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("buyer to seller"));

        mockMvc.perform(post("/api/chat/conversations/24/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"buyer to seller\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.receiverUserId").value(2402));

        verify(realtimePushService).pushToUsers(
                eq(List.of(2401L, 2402L)),
                eq("CHAT_MESSAGE"),
                eq(message));
    }

    @Test
    void realtimeFailureDoesNotTurnAPersistedMessageIntoAnApiFailure() throws Exception {
        ChatMessageVO message = message(102L, "persist before push");
        when(chatService.sendMessage(2401L, 24L, "persist before push")).thenReturn(message);
        doThrow(new IllegalStateException("realtime unavailable"))
                .when(realtimePushService)
                .pushToUsers(any(), eq("CHAT_MESSAGE"), eq(message));

        mockMvc.perform(post("/api/chat/conversations/24/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"persist before push\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(102));
    }

    @Test
    void blankAndOversizedMessagesAreRejectedBeforeTheServiceWrites() throws Exception {
        mockMvc.perform(post("/api/chat/conversations/24/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        String oversized = "x".repeat(1001);
        mockMvc.perform(post("/api/chat/conversations/24/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + oversized + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(chatService, never()).sendMessage(any(), any(), any());
    }

    private ChatConversationVO conversation(Long id) {
        ChatConversationVO conversation = new ChatConversationVO();
        conversation.setId(id);
        conversation.setSourceType("PRODUCT");
        conversation.setSourceId(2401L);
        conversation.setSourceTitle("UC24 product");
        return conversation;
    }

    private ChatMessageVO message(Long id, String content) {
        ChatMessageVO message = new ChatMessageVO();
        message.setId(id);
        message.setConversationId(24L);
        message.setSenderUserId(2401L);
        message.setReceiverUserId(2402L);
        message.setContent(content);
        message.setIsRead(0);
        return message;
    }
}

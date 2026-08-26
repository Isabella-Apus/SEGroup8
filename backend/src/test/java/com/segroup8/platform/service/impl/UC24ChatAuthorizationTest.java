package com.segroup8.platform.service.impl;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.entity.ChatConversation;
import com.segroup8.platform.mapper.ChatConversationMapper;
import com.segroup8.platform.mapper.ChatMessageMapper;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.UserBlockMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UC24ChatAuthorizationTest {

    @Mock private ChatConversationMapper chatConversationMapper;
    @Mock private ChatMessageMapper chatMessageMapper;
    @Mock private UserMapper userMapper;
    @Mock private ProductMapper productMapper;
    @Mock private ShopMapper shopMapper;
    @Mock private SecondhandProductMapper secondhandProductMapper;
    @Mock private UserBlockMapper userBlockMapper;
    @Mock private NotificationService notificationService;

    private ChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatServiceImpl(chatConversationMapper, chatMessageMapper, userMapper,
                productMapper, shopMapper, secondhandProductMapper, userBlockMapper, notificationService);
        ChatConversation conversation = new ChatConversation();
        conversation.setId(24L);
        conversation.setBuyerUserId(1L);
        conversation.setSellerUserId(2L);
        when(chatConversationMapper.selectById(24L)).thenReturn(conversation);
    }

    @Test
    void unitUc24001_nonParticipantCannotReadMessages() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.listConversationMessages(3L, 24L));

        assertEquals(403, error.getCode());
        assertEquals("无权访问当前会话", error.getMessage());
        verifyNoInteractions(chatMessageMapper);
    }

    @Test
    void unitUc24002_nonParticipantCannotSendMessage() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.sendMessage(3L, 24L, "越权消息"));

        assertEquals(403, error.getCode());
        verify(chatMessageMapper, never()).insert(
                org.mockito.ArgumentMatchers.any(com.segroup8.platform.entity.ChatMessage.class));
        verify(chatConversationMapper, never()).updateById(
                org.mockito.ArgumentMatchers.any(ChatConversation.class));
    }
}

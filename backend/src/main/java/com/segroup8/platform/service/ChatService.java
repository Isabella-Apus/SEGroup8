package com.segroup8.platform.service;

import com.segroup8.platform.vo.ChatConversationVO;
import com.segroup8.platform.vo.ChatMessageVO;

import java.util.List;

public interface ChatService {

    ChatConversationVO createOrGetConversation(Long currentUserId, Long targetUserId, String sourceType, Long sourceId);

    List<ChatConversationVO> listMyConversations(Long currentUserId);

    List<ChatMessageVO> listConversationMessages(Long currentUserId, Long conversationId);

    ChatMessageVO sendMessage(Long senderUserId, Long conversationId, String content);
}

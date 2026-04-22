package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.entity.ChatConversation;
import com.segroup8.platform.entity.ChatMessage;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.ChatConversationMapper;
import com.segroup8.platform.mapper.ChatMessageMapper;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.ChatService;
import com.segroup8.platform.vo.ChatConversationVO;
import com.segroup8.platform.vo.ChatMessageVO;
import com.segroup8.platform.vo.ChatParticipantVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatConversationMapper chatConversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final ShopMapper shopMapper;
    private final SecondhandProductMapper secondhandProductMapper;

    public ChatServiceImpl(ChatConversationMapper chatConversationMapper,
            ChatMessageMapper chatMessageMapper,
            UserMapper userMapper,
            ProductMapper productMapper,
            ShopMapper shopMapper,
            SecondhandProductMapper secondhandProductMapper) {
        this.chatConversationMapper = chatConversationMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.shopMapper = shopMapper;
        this.secondhandProductMapper = secondhandProductMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO createOrGetConversation(Long currentUserId, Long targetUserId, String sourceType, Long sourceId) {
        User currentUser = requireUser(currentUserId);
        User targetUser = requireUser(targetUserId);
        if (Objects.equals(currentUserId, targetUserId)) {
            throw new BusinessException(400, "不能给自己发消息");
        }

        ConversationTarget target = resolveConversationTarget(currentUser, targetUser, sourceType, sourceId);
        ChatConversation conversation = chatConversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getBuyerUserId, target.buyerUserId())
                .eq(ChatConversation::getSellerUserId, target.sellerUserId())
                .eq(ChatConversation::getSourceType, target.sourceType())
                .eq(ChatConversation::getSourceId, target.sourceId())
                .last("limit 1"));
        if (conversation == null) {
            conversation = new ChatConversation();
            conversation.setBuyerUserId(target.buyerUserId());
            conversation.setSellerUserId(target.sellerUserId());
            conversation.setSourceType(target.sourceType());
            conversation.setSourceId(target.sourceId());
            conversation.setSourceTitle(target.sourceTitle());
            chatConversationMapper.insert(conversation);
        } else if (!Objects.equals(conversation.getSourceTitle(), target.sourceTitle())) {
            conversation.setSourceTitle(target.sourceTitle());
            chatConversationMapper.updateById(conversation);
        }
        return toConversationVO(currentUserId, conversation, Map.of(
                currentUserId, currentUser,
                targetUserId, targetUser));
    }

    @Override
    public List<ChatConversationVO> listMyConversations(Long currentUserId) {
        requireUser(currentUserId);
        List<ChatConversation> conversations = chatConversationMapper.selectList(new LambdaQueryWrapper<ChatConversation>()
                .and(wrapper -> wrapper.eq(ChatConversation::getBuyerUserId, currentUserId)
                        .or()
                        .eq(ChatConversation::getSellerUserId, currentUserId))
                .orderByDesc(ChatConversation::getLastMessageTime)
                .orderByDesc(ChatConversation::getId));
        if (conversations.isEmpty()) {
            return List.of();
        }
        Map<Long, User> userMap = loadConversationUsers(conversations);
        Map<Long, Integer> unreadMap = buildUnreadMap(currentUserId, conversations.stream()
                .map(ChatConversation::getId)
                .toList());
        return conversations.stream()
                .map(conversation -> {
                    ChatConversationVO vo = toConversationVO(currentUserId, conversation, userMap);
                    vo.setUnreadCount(unreadMap.getOrDefault(conversation.getId(), 0));
                    return vo;
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMessageVO> listConversationMessages(Long currentUserId, Long conversationId) {
        ChatConversation conversation = requireConversationParticipant(currentUserId, conversationId);
        chatMessageMapper.update(null, new LambdaUpdateWrapper<ChatMessage>()
                .set(ChatMessage::getIsRead, 1)
                .eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getReceiverUserId, currentUserId)
                .eq(ChatMessage::getIsRead, 0));
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByAsc(ChatMessage::getCreateTime)
                .orderByAsc(ChatMessage::getId));
        Map<Long, User> userMap = loadConversationUsers(List.of(conversation));
        return messages.stream()
                .map(message -> toMessageVO(message, userMap))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendMessage(Long senderUserId, Long conversationId, String content) {
        ChatConversation conversation = requireConversationParticipant(senderUserId, conversationId);
        String normalizedContent = normalizeContent(content);
        Long receiverUserId = Objects.equals(conversation.getBuyerUserId(), senderUserId)
                ? conversation.getSellerUserId()
                : conversation.getBuyerUserId();

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderUserId(senderUserId);
        message.setReceiverUserId(receiverUserId);
        message.setContent(normalizedContent);
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(message);

        conversation.setLastMessageContent(normalizedContent);
        conversation.setLastMessageTime(message.getCreateTime());
        chatConversationMapper.updateById(conversation);

        Map<Long, User> userMap = loadConversationUsers(List.of(conversation));
        return toMessageVO(message, userMap);
    }

    private ChatConversation requireConversationParticipant(Long currentUserId, Long conversationId) {
        if (conversationId == null) {
            throw new BusinessException(400, "会话不存在");
        }
        ChatConversation conversation = chatConversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(404, "会话不存在");
        }
        if (!Objects.equals(conversation.getBuyerUserId(), currentUserId)
                && !Objects.equals(conversation.getSellerUserId(), currentUserId)) {
            throw new BusinessException(403, "无权访问当前会话");
        }
        return conversation;
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(400, "消息内容不能为空");
        }
        String normalized = content.trim();
        if (normalized.length() > 1000) {
            throw new BusinessException(400, "消息内容不能超过1000字");
        }
        return normalized;
    }

    private ConversationTarget resolveConversationTarget(User currentUser, User targetUser, String sourceType, Long sourceId) {
        String normalizedType = StringUtils.hasText(sourceType)
                ? sourceType.trim().toUpperCase(Locale.ROOT)
                : "DIRECT";
        if (!Set.of("DIRECT", "PRODUCT", "SECONDHAND").contains(normalizedType)) {
            throw new BusinessException(400, "会话来源不支持");
        }

        if ("PRODUCT".equals(normalizedType)) {
            if (sourceId == null) {
                throw new BusinessException(400, "商品ID不能为空");
            }
            Product product = productMapper.selectById(sourceId);
            if (product == null) {
                throw new BusinessException(404, "商品不存在");
            }
            Shop shop = shopMapper.selectById(product.getShopId());
            if (shop == null || !Objects.equals(shop.getOwnerUserId(), targetUser.getId())) {
                throw new BusinessException(400, "目标用户不是该商品卖家");
            }
            return buildConversationTargetBySellerId(
                    currentUser.getId(),
                    targetUser.getId(),
                    shop.getOwnerUserId(),
                    normalizedType,
                    sourceId,
                    product.getName());
        }

        if ("SECONDHAND".equals(normalizedType)) {
            if (sourceId == null) {
                throw new BusinessException(400, "二手商品ID不能为空");
            }
            SecondhandProduct product = secondhandProductMapper.selectById(sourceId);
            if (product == null) {
                throw new BusinessException(404, "二手商品不存在");
            }
            if (!Objects.equals(product.getSellerUserId(), targetUser.getId())) {
                throw new BusinessException(400, "目标用户不是该商品卖家");
            }
            return buildConversationTargetBySellerId(
                    currentUser.getId(),
                    targetUser.getId(),
                    product.getSellerUserId(),
                    normalizedType,
                    sourceId,
                    product.getName());
        }

        String currentRole = StringUtils.hasText(currentUser.getRole()) ? currentUser.getRole().trim() : "";
        String targetRole = StringUtils.hasText(targetUser.getRole()) ? targetUser.getRole().trim() : "";
        Long buyerUserId = currentUser.getId();
        Long sellerUserId = targetUser.getId();
        if (RoleEnum.OFFICIAL_SELLER.name().equals(currentRole) || RoleEnum.SELLER.name().equals(currentRole)) {
            buyerUserId = targetUser.getId();
            sellerUserId = currentUser.getId();
        } else if (RoleEnum.OFFICIAL_SELLER.name().equals(targetRole) || RoleEnum.SELLER.name().equals(targetRole)) {
            buyerUserId = currentUser.getId();
            sellerUserId = targetUser.getId();
        }
        return new ConversationTarget(buyerUserId, sellerUserId, normalizedType, 0L, "站内聊天");
    }

    private ConversationTarget buildConversationTargetBySellerId(Long currentUserId, Long targetUserId, Long sellerUserId,
            String sourceType, Long sourceId, String sourceTitle) {
        if (sellerUserId == null) {
            throw new BusinessException(400, "未找到卖家信息");
        }
        if (Objects.equals(currentUserId, sellerUserId)) {
            if (Objects.equals(targetUserId, sellerUserId)) {
                throw new BusinessException(400, "不能给自己发消息");
            }
            return new ConversationTarget(targetUserId, sellerUserId, sourceType, sourceId, sourceTitle);
        }
        if (Objects.equals(targetUserId, sellerUserId)) {
            return new ConversationTarget(currentUserId, sellerUserId, sourceType, sourceId, sourceTitle);
        }
        throw new BusinessException(400, "目标用户不是该商品卖家");
    }

    private Map<Long, User> loadConversationUsers(List<ChatConversation> conversations) {
        Set<Long> userIds = conversations.stream()
                .flatMap(conversation -> List.of(conversation.getBuyerUserId(), conversation.getSellerUserId()).stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, User> userMap = new HashMap<>();
        userMapper.selectBatchIds(userIds).forEach(user -> userMap.put(user.getId(), user));
        return userMap;
    }

    private Map<Long, Integer> buildUnreadMap(Long currentUserId, List<Long> conversationIds) {
        if (conversationIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> unreadMap = new HashMap<>();
        chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .in(ChatMessage::getConversationId, conversationIds)
                        .eq(ChatMessage::getReceiverUserId, currentUserId)
                        .eq(ChatMessage::getIsRead, 0))
                .forEach(message -> unreadMap.merge(message.getConversationId(), 1, Integer::sum));
        return unreadMap;
    }

    private ChatConversationVO toConversationVO(Long currentUserId, ChatConversation conversation, Map<Long, User> userMap) {
        ChatConversationVO vo = new ChatConversationVO();
        vo.setId(conversation.getId());
        vo.setSourceType(conversation.getSourceType());
        vo.setSourceId(conversation.getSourceId());
        vo.setSourceTitle(conversation.getSourceTitle());
        vo.setLastMessageContent(conversation.getLastMessageContent());
        vo.setLastMessageTime(conversation.getLastMessageTime());
        Long otherUserId = Objects.equals(conversation.getBuyerUserId(), currentUserId)
                ? conversation.getSellerUserId()
                : conversation.getBuyerUserId();
        vo.setSelf(toParticipantVO(userMap.get(currentUserId), currentUserId));
        vo.setOther(toParticipantVO(userMap.get(otherUserId), otherUserId));
        vo.setUnreadCount(0);
        return vo;
    }

    private ChatMessageVO toMessageVO(ChatMessage message, Map<Long, User> userMap) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setSenderUserId(message.getSenderUserId());
        vo.setReceiverUserId(message.getReceiverUserId());
        vo.setContent(message.getContent());
        vo.setIsRead(message.getIsRead());
        vo.setCreateTime(message.getCreateTime());
        vo.setSender(toParticipantVO(userMap.get(message.getSenderUserId()), message.getSenderUserId()));
        return vo;
    }

    private ChatParticipantVO toParticipantVO(User user, Long userId) {
        ChatParticipantVO vo = new ChatParticipantVO();
        vo.setUserId(userId);
        if (user == null) {
            vo.setNickname("用户" + userId);
            return vo;
        }
        vo.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        return vo;
    }

    private record ConversationTarget(Long buyerUserId, Long sellerUserId, String sourceType, Long sourceId,
            String sourceTitle) {
    }
}

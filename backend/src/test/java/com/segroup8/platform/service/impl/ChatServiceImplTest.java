package com.segroup8.platform.service.impl;

import com.segroup8.platform.entity.ChatConversation;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.ChatConversationMapper;
import com.segroup8.platform.mapper.ChatMessageMapper;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.UserBlockMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.NotificationService;
import com.segroup8.platform.vo.ChatConversationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatConversationMapper chatConversationMapper;
    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ShopMapper shopMapper;
    @Mock
    private SecondhandProductMapper secondhandProductMapper;
    @Mock
    private UserBlockMapper userBlockMapper;
    @Mock
    private NotificationService notificationService;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(
                chatConversationMapper,
                chatMessageMapper,
                userMapper,
                productMapper,
                shopMapper,
                secondhandProductMapper,
                userBlockMapper,
                notificationService);
    }

    @Test
    void createConversation_shouldAllowSecondhandSellerToContactBuyer() {
        Long sellerUserId = 2L;
        Long buyerUserId = 3L;
        Long productId = 99L;

        User seller = user(sellerUserId, "seller");
        User buyer = user(buyerUserId, "buyer");
        when(userMapper.selectById(sellerUserId)).thenReturn(seller);
        when(userMapper.selectById(buyerUserId)).thenReturn(buyer);

        SecondhandProduct product = new SecondhandProduct();
        product.setId(productId);
        product.setSellerUserId(sellerUserId);
        product.setName("Used camera");
        when(secondhandProductMapper.selectById(productId)).thenReturn(product);
        when(chatConversationMapper.selectOne(any())).thenReturn(null);

        ChatConversationVO result = chatService.createOrGetConversation(
                sellerUserId, buyerUserId, "SECONDHAND", productId);

        ArgumentCaptor<ChatConversation> captor = ArgumentCaptor.forClass(ChatConversation.class);
        verify(chatConversationMapper).insert(captor.capture());
        assertEquals(buyerUserId, captor.getValue().getBuyerUserId());
        assertEquals(sellerUserId, captor.getValue().getSellerUserId());
        assertEquals(buyerUserId, result.getOther().getUserId());
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(username);
        user.setRole("USER");
        return user;
    }
}

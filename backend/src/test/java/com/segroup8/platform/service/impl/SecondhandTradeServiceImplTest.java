package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.OrderStatusEnum;
import com.segroup8.platform.dto.AuctionBidRequest;
import com.segroup8.platform.dto.BargainConfirmRequest;
import com.segroup8.platform.entity.AuctionLog;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.ProductAuction;
import com.segroup8.platform.entity.ProductNegotiation;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.mapper.AuctionLogMapper;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.ProductAuctionMapper;
import com.segroup8.platform.mapper.ProductNegotiationMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.realtime.RealtimePushService;
import com.segroup8.platform.service.ChatService;
import com.segroup8.platform.service.NotificationService;
import com.segroup8.platform.service.settlement.EscrowSettlementService;
import com.segroup8.platform.vo.ProductNegotiationVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("DOMAIN_D")
@Tag("UC18")
@Tag("UC19")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class SecondhandTradeServiceImplTest {

    @Mock
    private ProductNegotiationMapper productNegotiationMapper;

    @Mock
    private ProductAuctionMapper productAuctionMapper;

    @Mock
    private AuctionLogMapper auctionLogMapper;

    @Mock
    private SecondhandProductMapper secondhandProductMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private ChatService chatService;

    @Mock
    private RealtimePushService realtimePushService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EscrowSettlementService escrowSettlementService;

    private SecondhandTradeServiceImpl secondhandTradeService;

    @BeforeEach
    void setUp() {
        secondhandTradeService = new SecondhandTradeServiceImpl(productNegotiationMapper, productAuctionMapper,
                auctionLogMapper, secondhandProductMapper, userMapper, orderInfoMapper, orderItemMapper,
                chatService, realtimePushService, notificationService, escrowSettlementService);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void confirmBargain_shouldCreatePendingPayOrderWhenSellerAccepts() {
        UserContext.setUserId(3L);
        ProductNegotiation negotiation = negotiation("APPLIED");
        negotiation.setConversationId(21L);
        SecondhandProduct product = product();
        BargainConfirmRequest request = new BargainConfirmRequest();
        request.setNegotiationId(9L);
        request.setConfirmedPrice(new BigDecimal("80.00"));
        request.setCreateOrder(true);

        when(productNegotiationMapper.selectById(9L)).thenReturn(negotiation);
        when(secondhandProductMapper.selectById(7L)).thenReturn(product);
        when(productNegotiationMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);
        when(secondhandProductMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);
        doAnswer(invocation -> {
            OrderInfo order = invocation.getArgument(0);
            order.setId(700L);
            return 1;
        }).when(orderInfoMapper).insert(any(OrderInfo.class));

        ProductNegotiationVO vo = secondhandTradeService.confirmBargain(request);

        ArgumentCaptor<OrderInfo> orderCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        verify(orderInfoMapper).insert(orderCaptor.capture());
        assertEquals(0, orderCaptor.getValue().getPayStatus());
        assertEquals(OrderStatusEnum.PENDING_PAY.getCode(), orderCaptor.getValue().getOrderStatus());
        assertEquals(new BigDecimal("80.00"), orderCaptor.getValue().getTotalAmount());
        assertEquals("USED", vo.getStatus());
        assertEquals(700L, vo.getOrderId());
        verify(orderItemMapper).insert(any(OrderItem.class));
        verify(chatService).sendMessage(any(), any(), any());
    }

    @Test
    void rejectBargain_shouldUpdateStatusAndNotifyBuyer() {
        UserContext.setUserId(3L);
        ProductNegotiation negotiation = negotiation("APPLIED");
        negotiation.setConversationId(21L);
        SecondhandProduct product = product();

        when(productNegotiationMapper.selectById(9L)).thenReturn(negotiation);
        when(productNegotiationMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);
        when(secondhandProductMapper.selectById(7L)).thenReturn(product);

        ProductNegotiationVO vo = secondhandTradeService.rejectBargain(9L);

        assertEquals("REJECTED", vo.getStatus());
        verify(productNegotiationMapper).update(any(), any(UpdateWrapper.class));
        verify(chatService).sendMessage(any(), any(), any());
        verify(notificationService).createNotification(any(), any(), any(), any());
    }

    @Test
    void rejectBargain_shouldThrowWhenAlreadyHandled() {
        UserContext.setUserId(3L);
        when(productNegotiationMapper.selectById(9L)).thenReturn(negotiation("CONFIRMED"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> secondhandTradeService.rejectBargain(9L));

        assertEquals(400, ex.getCode());
        verify(productNegotiationMapper, never()).update(any(), any(UpdateWrapper.class));
    }

    @Test
    void placeBid_shouldRejectBidLowerThanMinimumIncrement() {
        UserContext.setUserId(5L);
        ProductAuction auction = ongoingAuction();
        AuctionBidRequest request = new AuctionBidRequest();
        request.setBidAmount(new BigDecimal("105.00"));
        when(productAuctionMapper.selectById(11L)).thenReturn(auction);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> secondhandTradeService.placeBid(11L, request));

        assertEquals(400, ex.getCode());
        verify(escrowSettlementService, never()).changePersonalBalance(any(), any(), any(), any(), any(), any());
        verify(auctionLogMapper, never()).insert(any(AuctionLog.class));
    }

    @Test
    void placeBid_shouldRejectEndedAuction() {
        UserContext.setUserId(5L);
        ProductAuction auction = ongoingAuction();
        auction.setEndTime(LocalDateTime.now().minusMinutes(1));
        AuctionBidRequest request = new AuctionBidRequest();
        request.setBidAmount(new BigDecimal("120.00"));
        when(productAuctionMapper.selectById(11L)).thenReturn(auction);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> secondhandTradeService.placeBid(11L, request));

        assertEquals(400, ex.getCode());
        verify(escrowSettlementService, never()).changePersonalBalance(any(), any(), any(), any(), any(), any());
    }

    @Test
    void settleExpiredAuctions_shouldCreateOnlyOneOrderForAlreadySettledAuction() {
        ProductAuction auction = ongoingAuction();
        auction.setEndTime(LocalDateTime.now().minusMinutes(1));
        auction.setCurrentBidderUserId(5L);
        SecondhandProduct product = product();

        when(productAuctionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(auction));
        when(productAuctionMapper.selectById(11L)).thenReturn(auction);
        when(secondhandProductMapper.selectById(7L)).thenReturn(product);
        when(secondhandProductMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);
        doAnswer(invocation -> {
            ProductAuction target = productAuctionMapper.selectById(11L);
            target.setStatus("FINISHED");
            return 1;
        }).when(productAuctionMapper).update(any(), any(UpdateWrapper.class));
        doAnswer(invocation -> {
            OrderInfo order = invocation.getArgument(0);
            order.setId(701L);
            return 1;
        }).when(orderInfoMapper).insert(any(OrderInfo.class));

        secondhandTradeService.settleExpiredAuctions();
        secondhandTradeService.settleExpiredAuctions();

        verify(orderInfoMapper).insert(any(OrderInfo.class));
        verify(orderItemMapper).insert(any(OrderItem.class));
    }

    private ProductNegotiation negotiation(String status) {
        ProductNegotiation negotiation = new ProductNegotiation();
        negotiation.setId(9L);
        negotiation.setProductId(7L);
        negotiation.setBuyerUserId(5L);
        negotiation.setSellerUserId(3L);
        negotiation.setProposedPrice(new BigDecimal("75.00"));
        negotiation.setStatus(status);
        return negotiation;
    }

    private SecondhandProduct product() {
        SecondhandProduct product = new SecondhandProduct();
        product.setId(7L);
        product.setSellerUserId(3L);
        product.setName("二手教材");
        product.setSalePrice(new BigDecimal("100.00"));
        product.setStatus(1);
        return product;
    }

    private ProductAuction ongoingAuction() {
        ProductAuction auction = new ProductAuction();
        auction.setId(11L);
        auction.setProductId(7L);
        auction.setSellerUserId(3L);
        auction.setStartPrice(new BigDecimal("90.00"));
        auction.setIncrementAmount(new BigDecimal("10.00"));
        auction.setCurrentPrice(new BigDecimal("100.00"));
        auction.setStartTime(LocalDateTime.now().minusMinutes(10));
        auction.setEndTime(LocalDateTime.now().plusMinutes(30));
        auction.setStatus("ONGOING");
        auction.setVersion(0);
        return auction;
    }
}

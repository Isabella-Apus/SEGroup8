package com.segroup8.platform.service.impl;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.OrderStatusEnum;
import com.segroup8.platform.common.ProductStatusEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.entity.OrderAfterSaleLog;
import com.segroup8.platform.mapper.AddressMapper;
import com.segroup8.platform.mapper.OrderAfterSaleLogMapper;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.ReviewMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.realtime.RealtimePushService;
import com.segroup8.platform.service.LogisticsService;
import com.segroup8.platform.service.NotificationService;
import com.segroup8.platform.service.VoucherService;
import com.segroup8.platform.service.settlement.EscrowSettlementService;
import com.segroup8.platform.vo.OrderVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderInfoMapper orderInfoMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private SecondhandProductMapper secondhandProductMapper;
    @Mock
    private ShopMapper shopMapper;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private OrderAfterSaleLogMapper orderAfterSaleLogMapper;
    @Mock
    private RealtimePushService realtimePushService;
    @Mock
    private LogisticsService logisticsService;
    @Mock
    private EscrowSettlementService escrowSettlementService;
    @Mock
    private VoucherService voucherService;
    @Mock
    private NotificationService notificationService;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderInfoMapper,
                orderItemMapper,
                productMapper,
                reviewMapper,
                secondhandProductMapper,
                shopMapper,
                addressMapper,
                orderAfterSaleLogMapper,
                realtimePushService,
                logisticsService,
                escrowSettlementService,
                voucherService,
                notificationService);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void approveRefundBySeller_shouldPersistDecisionUserAndRemark() {
        Long sellerUserId = 200L;
        Long orderId = 10L;
        UserContext.setUserId(sellerUserId);

        OrderInfo order = new OrderInfo();
        order.setId(orderId);
        order.setBuyerUserId(999L);
        order.setRefundStatus(1);

        when(orderInfoMapper.selectById(orderId)).thenReturn(order);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrderId(orderId);
        item.setProductType("NEW");
        item.setProductId(88L);
        item.setProductName("Test");
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        Product product = new Product();
        product.setId(88L);
        product.setShopId(66L);
        when(productMapper.selectById(88L)).thenReturn(product);

        Shop shop = new Shop();
        shop.setId(66L);
        shop.setOwnerUserId(sellerUserId);
        when(shopMapper.selectById(66L)).thenReturn(shop);

        when(orderInfoMapper.update(any(), any())).thenReturn(1);

        OrderVO vo = orderService.approveRefundBySeller(orderId);

        assertEquals(sellerUserId, vo.getRefundDecisionUserId());
        assertEquals("卖家同意退货", vo.getRefundDecisionRemark());
        assertEquals("SELLER", vo.getRefundDecisionSource());

        verify(orderAfterSaleLogMapper).insert(any(OrderAfterSaleLog.class));
    }

    @Test
    void getMyOrderDetail_shouldReturnRefundDecisionFieldsWhenPresent() {
        Long buyerUserId = 100L;
        Long orderId = 11L;
        UserContext.setUserId(buyerUserId);

        OrderInfo order = new OrderInfo();
        order.setId(orderId);
        order.setBuyerUserId(buyerUserId);
        order.setRefundStatus(3);
        order.setRefundDecisionUserId(999L);
        order.setRefundDecisionRemark("平台审核：不符合退货条件");
        order.setRefundDecisionTime(LocalDateTime.now());

        when(orderInfoMapper.selectById(orderId)).thenReturn(order);

        OrderItem item = new OrderItem();
        item.setId(2L);
        item.setOrderId(orderId);
        item.setProductType("SECONDHAND");
        item.setProductId(123L);
        item.setProductName("Secondhand");
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        SecondhandProduct secondhand = new SecondhandProduct();
        secondhand.setId(123L);
        secondhand.setConditionLevel("95新");
        when(secondhandProductMapper.selectById(123L)).thenReturn(secondhand);

        OrderVO vo = orderService.getMyOrderDetail(orderId);
        assertEquals(999L, vo.getRefundDecisionUserId());
        assertEquals("平台审核：不符合退货条件", vo.getRefundDecisionRemark());
        assertNotNull(vo.getRefundDecisionTime());
    }

    @Test
    void getMyOrderDetail_shouldThrowWhenNotOwner() {
        Long buyerUserId = 100L;
        Long orderId = 12L;
        UserContext.setUserId(buyerUserId);

        OrderInfo order = new OrderInfo();
        order.setId(orderId);
        order.setBuyerUserId(999L);
        when(orderInfoMapper.selectById(anyLong())).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.getMyOrderDetail(orderId));
        assertEquals(403, ex.getCode());
    }

    @Test
    void cancelMyOrder_shouldRestoreStockWhenUnpaidEvenIfOrderStatusNotPendingPay() {
        Long buyerUserId = 100L;
        Long orderId = 13L;
        Long productId = 88L;
        Long shopId = 66L;
        UserContext.setUserId(buyerUserId);

        OrderInfo order = new OrderInfo();
        order.setId(orderId);
        order.setBuyerUserId(buyerUserId);
        // 模拟脏数据：状态已变更但仍未支付，取消时仍应回补库存
        order.setOrderStatus(OrderStatusEnum.PENDING_SHIP.getCode());
        order.setPayStatus(0);
        order.setVersion(1);
        when(orderInfoMapper.selectById(orderId)).thenReturn(order);
        when(orderInfoMapper.update(any(), any())).thenReturn(1);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrderId(orderId);
        item.setProductType("NEW");
        item.setProductId(productId);
        item.setProductName("Test");
        item.setQuantity(2);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        Product product = new Product();
        product.setId(productId);
        product.setShopId(shopId);
        product.setStock(5);
        product.setStatus(ProductStatusEnum.OFF_SHELF.getCode());
        when(productMapper.selectById(productId)).thenReturn(product);

        Shop shop = new Shop();
        shop.setId(shopId);
        shop.setOwnerUserId(200L);
        when(shopMapper.selectById(shopId)).thenReturn(shop);

        orderService.cancelMyOrder(orderId);

        verify(productMapper).updateById(argThat((Product p) -> p != null
                && productId.equals(p.getId())
                && Integer.valueOf(7).equals(p.getStock())
                && Integer.valueOf(ProductStatusEnum.ON_SHELF.getCode()).equals(p.getStatus())));
    }

    @Test
    void shipSellerOrder_shouldPersistNotificationForBuyer() {
        Long sellerUserId = 200L;
        Long buyerUserId = 100L;
        Long orderId = 14L;
        Long productId = 89L;
        Long shopId = 67L;
        UserContext.setUserId(sellerUserId);

        OrderInfo order = new OrderInfo();
        order.setId(orderId);
        order.setOrderNo("ORDER-14");
        order.setBuyerUserId(buyerUserId);
        order.setOrderStatus(OrderStatusEnum.PENDING_SHIP.getCode());
        when(orderInfoMapper.selectById(orderId)).thenReturn(order);

        OrderItem item = new OrderItem();
        item.setId(3L);
        item.setOrderId(orderId);
        item.setProductType("NEW");
        item.setProductId(productId);
        item.setProductName("Test product");
        item.setQuantity(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        Product product = new Product();
        product.setId(productId);
        product.setShopId(shopId);
        when(productMapper.selectById(productId)).thenReturn(product);

        Shop shop = new Shop();
        shop.setId(shopId);
        shop.setOwnerUserId(sellerUserId);
        when(shopMapper.selectById(shopId)).thenReturn(shop);

        orderService.shipSellerOrder(orderId);

        verify(notificationService).createNotification(
                eq(buyerUserId), anyString(), anyString(), eq("/order/" + orderId));
    }
}

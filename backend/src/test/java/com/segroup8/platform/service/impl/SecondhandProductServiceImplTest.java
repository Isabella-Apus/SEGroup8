package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.SecondhandOrderCreateRequest;
import com.segroup8.platform.dto.SecondhandProductPageQueryRequest;
import com.segroup8.platform.dto.SecondhandProductSaveRequest;
import com.segroup8.platform.entity.Address;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.ProductAuctionMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.AddressMapper;
import com.segroup8.platform.mapper.UserBlockMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.BrowseHistoryService;
import com.segroup8.platform.service.CategoryService;
import com.segroup8.platform.service.ProductRiskAuditService;
import com.segroup8.platform.service.SecondhandTradeService;
import com.segroup8.platform.service.SellerRatingAssembler;
import com.segroup8.platform.vo.OrderVO;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.SecondhandProductVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("DOMAIN_D")
@Tag("UC16")
@Tag("UC17")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class SecondhandProductServiceImplTest {

    @Mock
    private SecondhandProductMapper secondhandProductMapper;

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private ProductAuctionMapper productAuctionMapper;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private BrowseHistoryService browseHistoryService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CategoryService categoryService;

    @Mock
    private SecondhandTradeService secondhandTradeService;

    @Mock
    private ProductRiskAuditService productRiskAuditService;

    private SecondhandProductServiceImpl secondhandProductService;

    @Mock
    private UserBlockMapper userBlockMapper;

    @Mock
    private SellerRatingAssembler sellerRatingAssembler;

    @BeforeEach
    void setUp() {
        secondhandProductService = new SecondhandProductServiceImpl(secondhandProductMapper, orderInfoMapper,
                orderItemMapper, productAuctionMapper, userMapper, addressMapper, browseHistoryService, userBlockMapper,
                categoryService,
                secondhandTradeService, productRiskAuditService, sellerRatingAssembler);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void pagePublicProducts_shouldThrowWhenPriceRangeInvalid() {
        SecondhandProductPageQueryRequest request = new SecondhandProductPageQueryRequest();
        request.setMinPrice(new BigDecimal("500"));
        request.setMaxPrice(new BigDecimal("100"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> secondhandProductService.pagePublicProducts(request));
        assertEquals(400, ex.getCode());
    }

    @Test
    void createSellerProduct_shouldThrowWhenOriginPriceLowerThanSalePrice() {
        UserContext.setUserId(3L);
        SecondhandProductSaveRequest request = new SecondhandProductSaveRequest();
        request.setName("test");
        request.setOriginPrice(new BigDecimal("100"));
        request.setSalePrice(new BigDecimal("150"));
        request.setCategoryId(1);
        request.setSubCategoryId(101);
        request.setIsNegotiable(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> secondhandProductService.createSellerProduct(request));
        assertEquals(400, ex.getCode());
        verify(secondhandProductMapper, never()).insert(any(SecondhandProduct.class));
    }

    @Test
    void pageSellerProducts_shouldFilterByCurrentUser() {
        UserContext.setUserId(3L);
        SecondhandProductPageQueryRequest request = new SecondhandProductPageQueryRequest();
        request.setPageNum(1L);
        request.setPageSize(10L);

        SecondhandProduct product = new SecondhandProduct();
        product.setId(1L);
        product.setSellerUserId(3L);
        product.setName("Used Bicycle");
        product.setSalePrice(new BigDecimal("650"));
        product.setStatus(1);

        Page<SecondhandProduct> page = new Page<>(1, 10);
        page.setRecords(List.of(product));
        page.setTotal(1);
        when(secondhandProductMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageVO<SecondhandProductVO> vo = secondhandProductService.pageSellerProducts(request);

        assertEquals(1, vo.getTotal());
        assertEquals("Used Bicycle", vo.getRecords().get(0).getName());
    }

    @Test
    void buySecondhandProduct_shouldThrowWhenBuyerIsSeller() {
        UserContext.setUserId(3L);
        SecondhandProduct product = new SecondhandProduct();
        product.setId(1L);
        product.setSellerUserId(3L);
        product.setStatus(1);
        when(secondhandProductMapper.selectById(1L)).thenReturn(product);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> secondhandProductService.buySecondhandProduct(1L, new SecondhandOrderCreateRequest()));
        assertEquals(400, ex.getCode());
    }

    @Test
    void buySecondhandProduct_shouldCreateOrderWhenSuccess() {
        UserContext.setUserId(5L);
        SecondhandProduct product = new SecondhandProduct();
        product.setId(2L);
        product.setSellerUserId(3L);
        product.setName("Spare Headphones");
        product.setSalePrice(new BigDecimal("180"));
        product.setStatus(1);
        when(secondhandProductMapper.selectById(2L)).thenReturn(product);
        when(secondhandProductMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);
        when(secondhandTradeService.resolveEffectivePriceForBuyer(2L, 5L)).thenReturn(null);
        when(addressMapper.selectById(1L)).thenReturn(ownedAddress(1L, 5L));

        OrderVO vo = secondhandProductService.buySecondhandProduct(2L, orderRequest(1L));

        ArgumentCaptor<OrderInfo> orderCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        verify(orderInfoMapper).insert(orderCaptor.capture());
        assertEquals(5L, orderCaptor.getValue().getBuyerUserId());
        assertEquals(new BigDecimal("180"), vo.getTotalAmount());
    }

    @Test
    void buySecondhandProduct_shouldGenerateUniqueOrderNumbersForRapidPurchases() {
        UserContext.setUserId(5L);

        SecondhandProduct firstProduct = new SecondhandProduct();
        firstProduct.setId(2L);
        firstProduct.setSellerUserId(3L);
        firstProduct.setName("Spare Headphones");
        firstProduct.setSalePrice(new BigDecimal("180"));
        firstProduct.setStatus(1);

        SecondhandProduct secondProduct = new SecondhandProduct();
        secondProduct.setId(3L);
        secondProduct.setSellerUserId(3L);
        secondProduct.setName("Used Keyboard");
        secondProduct.setSalePrice(new BigDecimal("120"));
        secondProduct.setStatus(1);

        when(secondhandProductMapper.selectById(2L)).thenReturn(firstProduct);
        when(secondhandProductMapper.selectById(3L)).thenReturn(secondProduct);
        when(secondhandProductMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);
        when(secondhandTradeService.resolveEffectivePriceForBuyer(2L, 5L)).thenReturn(null);
        when(secondhandTradeService.resolveEffectivePriceForBuyer(3L, 5L)).thenReturn(null);
        when(addressMapper.selectById(1L)).thenReturn(ownedAddress(1L, 5L));

        secondhandProductService.buySecondhandProduct(2L, orderRequest(1L));
        secondhandProductService.buySecondhandProduct(3L, orderRequest(1L));

        ArgumentCaptor<OrderInfo> orderCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        verify(orderInfoMapper, times(2)).insert(orderCaptor.capture());
        List<OrderInfo> insertedOrders = orderCaptor.getAllValues();
        assertNotEquals(insertedOrders.get(0).getOrderNo(), insertedOrders.get(1).getOrderNo());
    }

    private SecondhandOrderCreateRequest orderRequest(Long addressId) {
        SecondhandOrderCreateRequest request = new SecondhandOrderCreateRequest();
        request.setAddressId(addressId);
        return request;
    }

    private Address ownedAddress(Long addressId, Long userId) {
        Address address = new Address();
        address.setId(addressId);
        address.setUserId(userId);
        address.setReceiverName("Buyer");
        address.setReceiverPhone("13800000000");
        address.setProvince("Guangdong");
        address.setCity("Guangzhou");
        address.setDetailAddress("Test Road 1");
        return address;
    }
}

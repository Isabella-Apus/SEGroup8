package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.ProductStatusEnum;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.ProductPageQueryRequest;
import com.segroup8.platform.dto.ProductSaveRequest;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.ProductVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ShopMapper shopMapper;

    @Mock
    private UserMapper userMapper;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productMapper, shopMapper, userMapper);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void pagePublicProducts_shouldThrowWhenPriceRangeInvalid() {
        ProductPageQueryRequest request = new ProductPageQueryRequest();
        request.setMinPrice(new BigDecimal("20"));
        request.setMaxPrice(new BigDecimal("10"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> productService.pagePublicProducts(request));
        assertEquals(400, ex.getCode());
        verify(productMapper, never()).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void getPublicProductDetail_shouldThrowWhenProductNotOnShelf() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> productService.getPublicProductDetail(1L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void createSellerProduct_shouldPersistWithCurrentSellerShopId() {
        Long userId = 10L;
        Long shopId = 100L;
        UserContext.setUserId(userId);

        User user = new User();
        user.setId(userId);
        user.setRole(RoleEnum.SELLER.name());
        when(userMapper.selectById(userId)).thenReturn(user);

        Shop shop = new Shop();
        shop.setId(shopId);
        shop.setOwnerUserId(userId);
        when(shopMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(shop);

        ProductSaveRequest request = new ProductSaveRequest();
        request.setName(" Test Product ");
        request.setCover("cover.jpg");
        request.setDescription("desc");
        request.setPrice(new BigDecimal("9.99"));
        request.setStock(5);
        request.setStatus(ProductStatusEnum.ON_SHELF.getCode());

        Product inserted = new Product();
        inserted.setId(1L);
        inserted.setShopId(shopId);
        inserted.setName("Test Product");
        inserted.setCover("cover.jpg");
        inserted.setDescription("desc");
        inserted.setPrice(new BigDecimal("9.99"));
        inserted.setStock(5);
        inserted.setStatus(ProductStatusEnum.ON_SHELF.getCode());

        ArgumentCaptor<Product> insertCaptor = ArgumentCaptor.forClass(Product.class);
        when(productMapper.selectById(1L)).thenReturn(inserted);

        productService.createSellerProduct(request);

        verify(productMapper).insert(insertCaptor.capture());
        Product toInsert = insertCaptor.getValue();
        assertEquals(shopId, toInsert.getShopId());
        assertEquals("Test Product", toInsert.getName());
        assertEquals(new BigDecimal("9.99"), toInsert.getPrice());
        assertEquals(5, toInsert.getStock());
    }

    @Test
    void adjustSellerProductStock_shouldThrowWhenResultNegative() {
        Long userId = 10L;
        Long shopId = 100L;
        UserContext.setUserId(userId);

        User user = new User();
        user.setId(userId);
        user.setRole(RoleEnum.SELLER.name());
        when(userMapper.selectById(userId)).thenReturn(user);

        Shop shop = new Shop();
        shop.setId(shopId);
        shop.setOwnerUserId(userId);
        when(shopMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(shop);

        Product product = new Product();
        product.setId(1L);
        product.setShopId(shopId);
        product.setStock(2);
        when(productMapper.selectById(1L)).thenReturn(product);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> productService.adjustSellerProductStock(1L, -3));
        assertEquals(400, ex.getCode());
        verify(productMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void pageSellerProducts_shouldUseCurrentSellerShopIdAndReturnPageVO() {
        Long userId = 10L;
        Long shopId = 100L;
        UserContext.setUserId(userId);

        User user = new User();
        user.setId(userId);
        user.setRole(RoleEnum.SELLER.name());
        when(userMapper.selectById(userId)).thenReturn(user);

        Shop shop = new Shop();
        shop.setId(shopId);
        shop.setOwnerUserId(userId);
        when(shopMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(shop);

        ProductPageQueryRequest request = new ProductPageQueryRequest();
        request.setPageNum(1L);
        request.setPageSize(10L);
        request.setMinPrice(new BigDecimal("1"));
        request.setMaxPrice(new BigDecimal("100"));

        Product p = new Product();
        p.setId(1L);
        p.setShopId(shopId);
        p.setName("P1");
        p.setPrice(new BigDecimal("10"));
        p.setStock(1);
        p.setStatus(ProductStatusEnum.ON_SHELF.getCode());

        Page<Product> mpPage = new Page<>(1, 10);
        mpPage.setRecords(List.of(p));
        mpPage.setTotal(1);

        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mpPage);

        PageVO<ProductVO> vo = productService.pageSellerProducts(request);

        assertEquals(1, vo.getTotal());
        assertNotNull(vo.getRecords());
        assertEquals(1, vo.getRecords().size());
        assertEquals("P1", vo.getRecords().get(0).getName());
    }
}


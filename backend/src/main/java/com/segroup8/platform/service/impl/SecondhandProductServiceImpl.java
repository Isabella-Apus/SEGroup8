package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.SecondhandOrderCreateRequest;
import com.segroup8.platform.dto.SecondhandProductPageQueryRequest;
import com.segroup8.platform.dto.SecondhandProductSaveRequest;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.service.SecondhandProductService;
import com.segroup8.platform.vo.OrderItemVO;
import com.segroup8.platform.vo.OrderVO;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.SecondhandProductVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
public class SecondhandProductServiceImpl implements SecondhandProductService {

    private static final int ON_SHELF = 1;
    private static final int OFF_SHELF = 2;
    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SecondhandProductMapper secondhandProductMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;

    public SecondhandProductServiceImpl(SecondhandProductMapper secondhandProductMapper, OrderInfoMapper orderInfoMapper,
            OrderItemMapper orderItemMapper) {
        this.secondhandProductMapper = secondhandProductMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    public PageVO<SecondhandProductVO> pagePublicProducts(SecondhandProductPageQueryRequest request) {
        validatePriceRange(request.getMinPrice(), request.getMaxPrice());
        LambdaQueryWrapper<SecondhandProduct> wrapper = new LambdaQueryWrapper<SecondhandProduct>()
                .eq(SecondhandProduct::getStatus, ON_SHELF)
                .orderByDesc(SecondhandProduct::getCreateTime);
        appendCommonFilters(wrapper, request);
        Page<SecondhandProduct> page = secondhandProductMapper.selectPage(
                Page.of(request.getPageNum(), request.getPageSize()),
                wrapper);
        return toPageVO(page);
    }

    @Override
    public SecondhandProductVO getPublicProductDetail(Long productId) {
        SecondhandProduct product = secondhandProductMapper.selectOne(new LambdaQueryWrapper<SecondhandProduct>()
                .eq(SecondhandProduct::getId, productId)
                .eq(SecondhandProduct::getStatus, ON_SHELF)
                .last("limit 1"));
        if (product == null) {
            throw new BusinessException(404, "二手商品不存在或已下架");
        }
        return toVO(product);
    }

    @Override
    public PageVO<SecondhandProductVO> pageSellerProducts(SecondhandProductPageQueryRequest request) {
        validatePriceRange(request.getMinPrice(), request.getMaxPrice());
        Long sellerUserId = requireUserId();
        LambdaQueryWrapper<SecondhandProduct> wrapper = new LambdaQueryWrapper<SecondhandProduct>()
                .eq(SecondhandProduct::getSellerUserId, sellerUserId)
                .orderByDesc(SecondhandProduct::getCreateTime);
        appendCommonFilters(wrapper, request);
        if (request.getStatus() != null) {
            wrapper.eq(SecondhandProduct::getStatus, request.getStatus());
        }
        Page<SecondhandProduct> page = secondhandProductMapper.selectPage(
                Page.of(request.getPageNum(), request.getPageSize()),
                wrapper);
        return toPageVO(page);
    }

    @Override
    public SecondhandProductVO createSellerProduct(SecondhandProductSaveRequest request) {
        validatePriceFields(request.getOriginPrice(), request.getSalePrice());
        Long sellerUserId = requireUserId();
        SecondhandProduct product = new SecondhandProduct();
        product.setSellerUserId(sellerUserId);
        product.setName(request.getName().trim());
        product.setCover(request.getCover());
        product.setDescription(request.getDescription());
        product.setOriginPrice(request.getOriginPrice());
        product.setSalePrice(request.getSalePrice());
        product.setConditionLevel(request.getConditionLevel());
        product.setStatus(normalizeStatus(request.getStatus(), ON_SHELF));
        secondhandProductMapper.insert(product);
        return toVO(secondhandProductMapper.selectById(product.getId()));
    }

    @Override
    public SecondhandProductVO updateSellerProduct(Long productId, SecondhandProductSaveRequest request) {
        validatePriceFields(request.getOriginPrice(), request.getSalePrice());
        SecondhandProduct product = getSellerOwnedProduct(productId);
        product.setName(request.getName().trim());
        product.setCover(request.getCover());
        product.setDescription(request.getDescription());
        product.setOriginPrice(request.getOriginPrice());
        product.setSalePrice(request.getSalePrice());
        product.setConditionLevel(request.getConditionLevel());
        if (request.getStatus() != null) {
            product.setStatus(normalizeStatus(request.getStatus(), null));
        }
        secondhandProductMapper.updateById(product);
        return toVO(secondhandProductMapper.selectById(productId));
    }

    @Override
    public void deleteSellerProduct(Long productId) {
        SecondhandProduct product = getSellerOwnedProduct(productId);
        secondhandProductMapper.deleteById(product.getId());
    }

    @Override
    public SecondhandProductVO changeSellerProductStatus(Long productId, Integer status) {
        SecondhandProduct product = getSellerOwnedProduct(productId);
        product.setStatus(normalizeStatus(status, null));
        secondhandProductMapper.updateById(product);
        return toVO(secondhandProductMapper.selectById(productId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO buySecondhandProduct(Long productId, SecondhandOrderCreateRequest request) {
        Long buyerUserId = requireUserId();
        SecondhandProduct product = secondhandProductMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(404, "二手商品不存在");
        }
        if (Objects.equals(product.getSellerUserId(), buyerUserId)) {
            throw new BusinessException(400, "不能购买自己发布的二手商品");
        }
        if (!Objects.equals(product.getStatus(), ON_SHELF)) {
            throw new BusinessException(400, "二手商品已下架或已售出");
        }

        int updated = secondhandProductMapper.update(null, new UpdateWrapper<SecondhandProduct>()
                .set("status", OFF_SHELF)
                .eq("id", productId)
                .eq("status", ON_SHELF));
        if (updated == 0) {
            throw new BusinessException(400, "二手商品已售出");
        }

        OrderInfo order = new OrderInfo();
        order.setOrderNo(generateOrderNo(buyerUserId));
        order.setBuyerUserId(buyerUserId);
        order.setTotalAmount(product.getSalePrice());
        order.setPayStatus(1);
        order.setOrderStatus(1);
        order.setRemark(request == null ? null : request.getRemark());
        order.setCreateTime(LocalDateTime.now());
        orderInfoMapper.insert(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setProductType("SECONDHAND");
        orderItem.setProductId(product.getId());
        orderItem.setProductName(product.getName());
        orderItem.setPrice(product.getSalePrice());
        orderItem.setQuantity(1);
        orderItem.setStatus(1);
        orderItemMapper.insert(orderItem);

        return toOrderVO(order, orderItem);
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }

    private void appendCommonFilters(LambdaQueryWrapper<SecondhandProduct> wrapper,
            SecondhandProductPageQueryRequest request) {
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(SecondhandProduct::getName, request.getKeyword().trim());
        }
        if (request.getMinPrice() != null) {
            wrapper.ge(SecondhandProduct::getSalePrice, request.getMinPrice());
        }
        if (request.getMaxPrice() != null) {
            wrapper.le(SecondhandProduct::getSalePrice, request.getMaxPrice());
        }
        if (StringUtils.hasText(request.getConditionLevel())) {
            wrapper.eq(SecondhandProduct::getConditionLevel, request.getConditionLevel().trim());
        }
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException(400, "最低价不能大于最高价");
        }
    }

    private void validatePriceFields(BigDecimal originPrice, BigDecimal salePrice) {
        if (originPrice != null && salePrice != null && originPrice.compareTo(salePrice) < 0) {
            throw new BusinessException(400, "原价不能低于售价");
        }
    }

    private Integer normalizeStatus(Integer status, Integer fallback) {
        Integer target = status == null ? fallback : status;
        if (!Objects.equals(target, ON_SHELF) && !Objects.equals(target, OFF_SHELF)) {
            throw new BusinessException(400, "二手商品状态非法");
        }
        return target;
    }

    private SecondhandProduct getSellerOwnedProduct(Long productId) {
        Long sellerUserId = requireUserId();
        SecondhandProduct product = secondhandProductMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(404, "二手商品不存在");
        }
        if (!Objects.equals(product.getSellerUserId(), sellerUserId)) {
            throw new BusinessException(403, "无权操作该二手商品");
        }
        return product;
    }

    private PageVO<SecondhandProductVO> toPageVO(Page<SecondhandProduct> page) {
        PageVO<SecondhandProductVO> vo = new PageVO<>();
        vo.setTotal(page.getTotal());
        vo.setPageNum(page.getCurrent());
        vo.setPageSize(page.getSize());
        vo.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return vo;
    }

    private SecondhandProductVO toVO(SecondhandProduct product) {
        SecondhandProductVO vo = new SecondhandProductVO();
        vo.setId(product.getId());
        vo.setSellerUserId(product.getSellerUserId());
        vo.setName(product.getName());
        vo.setCover(product.getCover());
        vo.setDescription(product.getDescription());
        vo.setOriginPrice(product.getOriginPrice());
        vo.setSalePrice(product.getSalePrice());
        vo.setConditionLevel(product.getConditionLevel());
        vo.setStatus(product.getStatus());
        vo.setStatusName(Objects.equals(product.getStatus(), ON_SHELF) ? "在售" : "下架");
        vo.setCreateTime(product.getCreateTime());
        return vo;
    }

    private String generateOrderNo(Long userId) {
        String timePart = LocalDateTime.now().format(ORDER_NO_FORMATTER);
        return "SND" + timePart + String.format("%04d", userId % 10000);
    }

    private OrderVO toOrderVO(OrderInfo order, OrderItem item) {
        OrderItemVO itemVO = new OrderItemVO();
        itemVO.setId(item.getId());
        itemVO.setProductId(item.getProductId());
        itemVO.setProductName(item.getProductName());
        itemVO.setPrice(item.getPrice());
        itemVO.setQuantity(item.getQuantity());

        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayStatus(order.getPayStatus());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(List.of(itemVO));
        return vo;
    }
}


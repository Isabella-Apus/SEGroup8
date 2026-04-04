package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.ProductStatusEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.CreateOrderItemRequest;
import com.segroup8.platform.dto.CreateOrderRequest;
import com.segroup8.platform.dto.OrderPageQueryRequest;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.service.OrderService;
import com.segroup8.platform.vo.OrderItemVO;
import com.segroup8.platform.vo.OrderVO;
import com.segroup8.platform.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;

    public OrderServiceImpl(OrderInfoMapper orderInfoMapper, OrderItemMapper orderItemMapper,
            ProductMapper productMapper) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(CreateOrderRequest request) {
        Long userId = requireUserId();
        Map<Long, Integer> merged = mergeItems(request.getItems());
        if (merged.isEmpty()) {
            throw new BusinessException(400, "订单商品不能为空");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : merged.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            Product product = productMapper.selectById(productId);
            if (product == null) {
                throw new BusinessException(404, "商品不存在: " + productId);
            }
            if (!ProductStatusEnum.ON_SHELF.equals(ProductStatusEnum.of(product.getStatus()))) {
                throw new BusinessException(400, "商品已下架: " + product.getName());
            }

            int updated = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .setSql("stock = stock - " + quantity)
                    .eq(Product::getId, productId)
                    .ge(Product::getStock, quantity));
            if (updated == 0) {
                throw new BusinessException(400, "库存不足: " + product.getName());
            }

            BigDecimal itemAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(itemAmount);

            OrderItem item = new OrderItem();
            item.setProductType("NEW");
            item.setProductId(productId);
            item.setProductName(product.getName());
            item.setPrice(product.getPrice());
            item.setQuantity(quantity);
            item.setStatus(1);
            orderItems.add(item);
        }

        OrderInfo order = new OrderInfo();
        order.setOrderNo(generateOrderNo(userId));
        order.setBuyerUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPayStatus(1);
        order.setOrderStatus(1);
        order.setRemark(request.getRemark());
        order.setCreateTime(LocalDateTime.now());
        orderInfoMapper.insert(order);

        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        return buildOrderVO(order, orderItems);
    }

    @Override
    public PageVO<OrderVO> pageMyOrders(OrderPageQueryRequest request) {
        Long userId = requireUserId();
        Page<OrderInfo> page = orderInfoMapper.selectPage(Page.of(request.getPageNum(), request.getPageSize()),
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getBuyerUserId, userId)
                        .orderByDesc(OrderInfo::getCreateTime));

        List<OrderVO> records = page.getRecords().stream().map(order -> {
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId())
                    .orderByAsc(OrderItem::getId));
            return buildOrderVO(order, items);
        }).toList();

        PageVO<OrderVO> vo = new PageVO<>();
        vo.setTotal(page.getTotal());
        vo.setPageNum(page.getCurrent());
        vo.setPageSize(page.getSize());
        vo.setRecords(records);
        return vo;
    }

    private Map<Long, Integer> mergeItems(List<CreateOrderItemRequest> items) {
        Map<Long, Integer> merged = new LinkedHashMap<>();
        for (CreateOrderItemRequest item : items) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            merged.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return merged;
    }

    private String generateOrderNo(Long userId) {
        String timePart = LocalDateTime.now().format(ORDER_NO_FORMATTER);
        return "ORD" + timePart + String.format("%04d", userId % 10000);
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }

    private OrderVO buildOrderVO(OrderInfo order, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayStatus(order.getPayStatus());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(items.stream().map(this::toItemVO).toList());
        return vo;
    }

    private OrderItemVO toItemVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(item.getId());
        vo.setProductId(item.getProductId());
        vo.setProductName(item.getProductName());
        vo.setPrice(item.getPrice());
        vo.setQuantity(item.getQuantity());
        return vo;
    }
}

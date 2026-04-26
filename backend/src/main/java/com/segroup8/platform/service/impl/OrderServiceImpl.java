package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.AccessControl;
import com.segroup8.platform.common.AfterSaleActionEnum;
import com.segroup8.platform.common.OperatorRoleEnum;
import com.segroup8.platform.common.OrderStateMachine;
import com.segroup8.platform.common.OrderStatusEnum;
import com.segroup8.platform.common.ProductStatusEnum;
import com.segroup8.platform.common.RefundDecisionSourceEnum;
import com.segroup8.platform.common.RefundStatusEnum;
import com.segroup8.platform.common.TransactionTradeTypeEnum;
import com.segroup8.platform.dto.CreateOrderItemRequest;
import com.segroup8.platform.dto.CreateOrderRequest;
import com.segroup8.platform.dto.PayOrderRequest;
import com.segroup8.platform.dto.OrderPageQueryRequest;
import com.segroup8.platform.dto.OrderItemReviewBatchSubmitRequest;
import com.segroup8.platform.dto.OrderItemReviewSubmitRequest;
import com.segroup8.platform.dto.OrderRefundApplyRequest;
import com.segroup8.platform.dto.OrderReviewSubmitRequest;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.entity.Review;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.entity.Address;
import com.segroup8.platform.entity.Notification;
import com.segroup8.platform.entity.OrderAfterSaleLog;
import com.segroup8.platform.entity.UserVoucher;
import com.segroup8.platform.entity.Voucher;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.OrderAfterSaleLogMapper;
import com.segroup8.platform.mapper.ReviewMapper;
import com.segroup8.platform.mapper.UserVoucherMapper;
import com.segroup8.platform.mapper.VoucherMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.AddressMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.realtime.RealtimePushService;
import com.segroup8.platform.service.OrderService;
import com.segroup8.platform.service.LogisticsService;
import com.segroup8.platform.service.NotificationService;
import com.segroup8.platform.service.settlement.EscrowSettlementService;
import com.segroup8.platform.vo.OrderItemVO;
import com.segroup8.platform.vo.OrderVO;
import com.segroup8.platform.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

@Service
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ReviewMapper reviewMapper;
    private final SecondhandProductMapper secondhandProductMapper;
    private final ShopMapper shopMapper;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final VoucherMapper voucherMapper;
    private final UserVoucherMapper userVoucherMapper;
    private final NotificationService notificationService;
    private final OrderAfterSaleLogMapper orderAfterSaleLogMapper;
    private final RealtimePushService realtimePushService;
    private final LogisticsService logisticsService;
    private final EscrowSettlementService escrowSettlementService;

    public OrderServiceImpl(OrderInfoMapper orderInfoMapper, OrderItemMapper orderItemMapper,
            ProductMapper productMapper, ReviewMapper reviewMapper, SecondhandProductMapper secondhandProductMapper,
            ShopMapper shopMapper,
            UserMapper userMapper,
            AddressMapper addressMapper,
            VoucherMapper voucherMapper,
            UserVoucherMapper userVoucherMapper,
            NotificationService notificationService,
            OrderAfterSaleLogMapper orderAfterSaleLogMapper,
            RealtimePushService realtimePushService, LogisticsService logisticsService,
            EscrowSettlementService escrowSettlementService) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.reviewMapper = reviewMapper;
        this.secondhandProductMapper = secondhandProductMapper;
        this.shopMapper = shopMapper;
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
        this.voucherMapper = voucherMapper;
        this.userVoucherMapper = userVoucherMapper;
        this.notificationService = notificationService;
        this.orderAfterSaleLogMapper = orderAfterSaleLogMapper;
        this.realtimePushService = realtimePushService;
        this.logisticsService = logisticsService;
        this.escrowSettlementService = escrowSettlementService;
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

            if (product.getStock() == null || product.getStock() < quantity) {
                throw new BusinessException(400, "库存不足: " + product.getName());
            }
            product.setStock(product.getStock() - quantity);
            if (product.getStock() <= 0) {
                product.setStatus(ProductStatusEnum.OFF_SHELF.getCode());
            }
            productMapper.updateById(product);

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
        order.setPayStatus(0);
        order.setOrderStatus(OrderStatusEnum.PENDING_PAY.getCode());
        order.setCanRefund(1);
        order.setLogisticsStatus("PENDING");
        order.setLogisticsCurrentIndex(0);
        order.setPayMethod("余额支付");
        order.setRemark(request.getRemark());
        order.setVoucherId(request.getVoucherId());
        applyVoucherIfNeeded(order, userId, totalAmount, request.getVoucherId());
        order.setCreateTime(LocalDateTime.now());

        Address addr;
        if (request.getAddressId() != null) {
            addr = addressMapper.selectById(request.getAddressId());
            if (addr == null || !Objects.equals(addr.getUserId(), userId)) {
                throw new BusinessException(400, "收货地址不存在或不属于当前用户");
            }
        } else {
            addr = addressMapper.selectOne(new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, userId)
                    .orderByDesc(Address::getIsDefault)
                    .orderByDesc(Address::getId)
                    .last("limit 1"));
        }
        if (addr == null) {
            throw new BusinessException(400, "请先选择收货地址");
        }
        order.setReceiverName(addr.getReceiverName());
        order.setReceiverPhone(addr.getReceiverPhone());
        order.setReceiverProvince(addr.getProvince());
        order.setReceiverCity(addr.getCity());
        order.setReceiverDetailAddress(addr.getDetailAddress());
        orderInfoMapper.insert(order);
        bindVoucherToOrderIfNeeded(order.getId(), userId, request.getVoucherId());

        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        return buildOrderVO(order, orderItems);
    }

    @Override
    public PageVO<OrderVO> pageMyOrders(OrderPageQueryRequest request) {
        Long userId = requireUserId();
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getBuyerUserId, userId)
                .eq(request.getOrderStatus() != null, OrderInfo::getOrderStatus, request.getOrderStatus())
                .eq(request.getRefundStatus() != null, OrderInfo::getRefundStatus, request.getRefundStatus())
                .orderByDesc(OrderInfo::getCreateTime);

        if (request.getStartTime() != null) {
            LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getStartTime()),
                    ZoneId.of("Asia/Shanghai"));
            wrapper.ge(OrderInfo::getCreateTime, start);
        }
        if (request.getEndTime() != null) {
            LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getEndTime()),
                    ZoneId.of("Asia/Shanghai"));
            wrapper.le(OrderInfo::getCreateTime, end);
        }
        if (request.getMinAmount() != null) {
            wrapper.ge(OrderInfo::getTotalAmount, BigDecimal.valueOf(request.getMinAmount()));
        }
        if (request.getMaxAmount() != null) {
            wrapper.le(OrderInfo::getTotalAmount, BigDecimal.valueOf(request.getMaxAmount()));
        }

        Page<OrderInfo> page = orderInfoMapper.selectPage(Page.of(request.getPageNum(), request.getPageSize()),
                wrapper);

        List<OrderVO> records = page.getRecords().stream().map(order -> {
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId())
                    .orderByAsc(OrderItem::getId));
            if (StringUtils.hasText(request.getKeyword())) {
                String kw = request.getKeyword().trim();
                boolean hitOrderNo = order.getOrderNo() != null && order.getOrderNo().contains(kw);
                boolean hitProduct = items.stream()
                        .anyMatch(i -> i.getProductName() != null && i.getProductName().contains(kw));
                if (!hitOrderNo && !hitProduct) {
                    return null;
                }
            }
            return buildOrderVO(order, items);
        }).filter(Objects::nonNull).toList();

        PageVO<OrderVO> vo = new PageVO<>();
        vo.setTotal(page.getTotal());
        vo.setPageNum(page.getCurrent());
        vo.setPageSize(page.getSize());
        vo.setRecords(records);
        return vo;
    }

    @Override
    public OrderVO getMyOrderDetail(Long orderId) {
        requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        AccessControl.requireOrderOwnedByBuyer(order, "无权查看该订单");
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        return buildOrderVO(order, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO payMyOrder(Long orderId, PayOrderRequest request) {
        Long userId = requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        AccessControl.requireOrderOwnedByBuyer(order, "无权操作该订单");
        OrderStateMachine.assertOrderActionAllowed(order, OrderStateMachine.OrderAction.PAY, "当前状态不可支付");
        LocalDateTime now = LocalDateTime.now();
        String payMode = request != null && StringUtils.hasText(request.getPayMode())
                ? request.getPayMode().trim().toUpperCase()
                : "THIRD_PARTY";
        String payChannel = request != null && StringUtils.hasText(request.getPayChannel())
                ? request.getPayChannel().trim().toUpperCase()
                : "WECHAT";
        String payMethod;
        BigDecimal payAmount = order.getPayableAmount() == null ? order.getTotalAmount() : order.getPayableAmount();
        if ("COIN".equals(payMode)) {
            escrowSettlementService.changePersonalBalance(userId,
                    payAmount.negate(),
                    orderId,
                    "COIN_PAY",
                    TransactionTradeTypeEnum.EXPENSE_PURCHASE,
                    "商城币支付订单");
            payMethod = "商城币支付";
        } else {
            payMethod = "ALIPAY".equals(payChannel) ? "支付宝支付" : "微信支付";
        }
        Integer version = normalizeVersion(order);
        int updated = orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                .set("pay_status", 1)
                .set("paid_time", now)
                .set("pay_method", payMethod)
                .set("order_status", OrderStatusEnum.PENDING_SHIP.getCode())
                .setSql("version = version + 1")
                .eq("id", orderId)
                .eq("buyer_user_id", userId)
                .eq("order_status", OrderStatusEnum.PENDING_PAY.getCode())
                .eq("version", version));
        if (updated == 0) {
            throw new BusinessException(400, "当前状态不可支付");
        }
        consumeVoucherOnPayIfNeeded(orderId, userId);
        pushOrderRealtime(orderId, userId, resolveSellerUserIds(orderId), "ORDER_STATUS_UPDATED", "订单已支付，等待卖家发货");
        return getMyOrderDetail(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO cancelMyOrder(Long orderId) {
        Long userId = requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        AccessControl.requireOrderOwnedByBuyer(order, "无权操作该订单");
        OrderStateMachine.assertOrderActionAllowed(order, OrderStateMachine.OrderAction.CANCEL, "当前状态不可取消");
        LocalDateTime now = LocalDateTime.now();
        Integer version = normalizeVersion(order);
        int updated = orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                .set("order_status", OrderStatusEnum.CLOSED.getCode())
                .set("closed_time", now)
                .setSql("version = version + 1")
                .eq("id", orderId)
                .eq("buyer_user_id", userId)
                .in("order_status",
                        OrderStatusEnum.PENDING_PAY.getCode(),
                        OrderStatusEnum.PENDING_SHIP.getCode(),
                        OrderStatusEnum.SHIPPED.getCode(),
                        OrderStatusEnum.RECEIVED.getCode())
                .eq("version", version));
        if (updated == 0) {
            throw new BusinessException(400, "当前状态不可取消");
        }
        boolean isUnpaid = order.getPayStatus() == null || Integer.valueOf(0).equals(order.getPayStatus());
        if (isUnpaid) {
            restoreStockForNewItems(orderId);
            rollbackVoucherIfNeeded(orderId, userId);
        }
        pushOrderRealtime(orderId, userId, resolveSellerUserIds(orderId), "ORDER_STATUS_UPDATED", "订单已取消");
        return getMyOrderDetail(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO confirmReceiveMyOrder(Long orderId) {
        Long userId = requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        AccessControl.requireOrderOwnedByBuyer(order, "无权操作该订单");
        OrderStateMachine.assertOrderActionAllowed(order, OrderStateMachine.OrderAction.CONFIRM_RECEIVE, "仅待收货订单可确认收货");
        LocalDateTime now = LocalDateTime.now();
        Integer version = normalizeVersion(order);
        int updated = orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                .set("order_status", OrderStatusEnum.RECEIVED.getCode())
                .set("received_time", now)
                .setSql("version = version + 1")
                .eq("id", orderId)
                .eq("buyer_user_id", userId)
                .eq("order_status", OrderStatusEnum.SHIPPED.getCode())
                .eq("version", version));
        if (updated == 0) {
            throw new BusinessException(400, "仅待收货订单可确认收货");
        }
        finalizeReceipt(orderId, order, now);
        pushOrderRealtime(orderId, userId, resolveSellerUserIds(orderId), "ORDER_STATUS_UPDATED", "买家已确认收货");
        return getMyOrderDetail(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoConfirmReceiveForSystem(Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        if (!Integer.valueOf(OrderStatusEnum.SHIPPED.getCode()).equals(order.getOrderStatus())) {
            return;
        }
        if (!"ARRIVED".equalsIgnoreCase(order.getLogisticsStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (order.getAutoConfirmDeadline() == null || now.isBefore(order.getAutoConfirmDeadline())) {
            return;
        }
        Integer version = normalizeVersion(order);
        int updated = orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                .set("order_status", OrderStatusEnum.RECEIVED.getCode())
                .set("received_time", now)
                .setSql("version = version + 1")
                .eq("id", orderId)
                .eq("order_status", OrderStatusEnum.SHIPPED.getCode())
                .eq("version", version));
        if (updated == 0) {
            return;
        }
        finalizeReceipt(orderId, order, now);
        pushOrderRealtime(orderId, order.getBuyerUserId(), resolveSellerUserIds(orderId), "ORDER_STATUS_UPDATED",
                "系统已自动确认收货");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoApproveRefundForSystem(Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        if (!Integer.valueOf(RefundStatusEnum.PROCESSING.getCode()).equals(order.getRefundStatus())) {
            return;
        }
        if (order.getRefundApplyTime() == null
                || LocalDateTime.now().isBefore(order.getRefundApplyTime().plusDays(7))) {
            return;
        }
        String refundMode = StringUtils.hasText(order.getRefundMode()) ? order.getRefundMode().trim().toUpperCase()
                : "RETURN_REFUND";
        if (!"RETURN_REFUND".equals(refundMode)) {
            return;
        }
        Integer version = normalizeVersion(order);
        LocalDateTime now = LocalDateTime.now();
        String decisionRemark = "卖家超时 7 天未处理，系统自动退款";
        int updated = orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                .set("refund_status", RefundStatusEnum.APPROVED.getCode())
                .set("order_status", OrderStatusEnum.CLOSED.getCode())
                .set("refund_decision_time", now)
                .set("refund_decision_user_id", 0L)
                .set("refund_decision_remark", decisionRemark)
                .set("refund_decision_source", RefundDecisionSourceEnum.SYSTEM.name())
                .set("closed_time", now)
                .set("can_refund", 0)
                .setSql("version = version + 1")
                .eq("id", orderId)
                .eq("refund_status", RefundStatusEnum.PROCESSING.getCode())
                .eq("version", version));
        if (updated == 0) {
            return;
        }
        escrowSettlementService.changePersonalBalance(
                order.getBuyerUserId(),
            resolveRefundAmount(order),
                orderId,
                "REFUND_TIMEOUT_AUTO",
                TransactionTradeTypeEnum.REFUND_BACKFLOW,
                decisionRemark);
        restoreConsumedVoucherOnRefundIfNeeded(order);
        insertAfterSaleLog(orderId, AfterSaleActionEnum.APPROVE, 0L, OperatorRoleEnum.ADMIN, decisionRemark);
        pushOrderRealtime(orderId, order.getBuyerUserId(), resolveSellerUserIds(orderId), "AFTER_SALE_UPDATED",
                "系统已自动退款");
    }

    private void finalizeReceipt(Long orderId, OrderInfo order, LocalDateTime now) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
        escrowSettlementService.releaseEscrow(order, items);
        distributeVoucherCostBeforeSettlement(order, items);
        boolean hasNewProduct = items.stream().anyMatch(i -> "NEW".equalsIgnoreCase(i.getProductType()));
        LocalDateTime afterSalesDeadline = hasNewProduct ? now.plusDays(7) : null;
        orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                .set("can_refund", hasNewProduct ? 1 : 0)
                .set("after_sales_deadline", afterSalesDeadline)
                .eq("id", orderId));
    }

    @Override
    public OrderVO completeMyOrder(Long orderId) {
        Long userId = requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        AccessControl.requireOrderOwnedByBuyer(order, "无权操作该订单");
        OrderStateMachine.assertOrderActionAllowed(order, OrderStateMachine.OrderAction.COMPLETE, "仅待评价订单可完成");
        LocalDateTime now = LocalDateTime.now();
        Integer version = normalizeVersion(order);
        int updated = orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                .set("order_status", OrderStatusEnum.COMPLETED.getCode())
                .set("completed_time", now)
                .setSql("version = version + 1")
                .eq("id", orderId)
                .eq("buyer_user_id", userId)
                .eq("order_status", OrderStatusEnum.RECEIVED.getCode())
                .eq("version", version));
        if (updated == 0) {
            throw new BusinessException(400, "仅待评价订单可完成");
        }
        pushOrderRealtime(orderId, userId, resolveSellerUserIds(orderId), "ORDER_STATUS_UPDATED", "订单已完成");
        return getMyOrderDetail(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO submitMyOrderReview(Long orderId, OrderReviewSubmitRequest request) {
        Long userId = requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        AccessControl.requireOrderOwnedByBuyer(order, "无权操作该订单");
        if (!Integer.valueOf(OrderStatusEnum.RECEIVED.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException(400, "仅待评价订单可提交评价");
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        if (items.isEmpty()) {
            throw new BusinessException(400, "订单商品为空，无法评价");
        }
        for (OrderItem item : items) {
            Long existing = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                    .eq(Review::getOrderId, order.getId())
                    .eq(Review::getUserId, userId)
                    .eq(Review::getProductType, item.getProductType())
                    .eq(Review::getProductId, item.getProductId()));
            if (existing != null && existing > 0) {
                throw new BusinessException(400, "该订单已评价，请勿重复提交");
            }
        }
        for (OrderItem item : items) {
            Review review = new Review();
            review.setOrderId(order.getId());
            review.setProductType(item.getProductType());
            review.setProductId(item.getProductId());
            review.setUserId(userId);
            review.setScore(request.getScore());
            review.setContent(request.getContent());
            review.setReviewType("ORIGINAL");
            review.setStatus(1);
            reviewMapper.insert(review);
        }
        order.setOrderStatus(OrderStatusEnum.COMPLETED.getCode());
        order.setCompletedTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);
        return buildOrderVO(order, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO submitMyOrderItemReviews(Long orderId, OrderItemReviewBatchSubmitRequest request) {
        Long userId = requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        AccessControl.requireOrderOwnedByBuyer(order, "无权操作该订单");
        if (!Integer.valueOf(OrderStatusEnum.RECEIVED.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException(400, "仅待评价订单可提交评价");
        }
        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        if (orderItems.isEmpty()) {
            throw new BusinessException(400, "订单商品为空，无法评价");
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(400, "评价明细不能为空");
        }

        for (OrderItemReviewSubmitRequest r : request.getItems()) {
            boolean exists = orderItems.stream().anyMatch(i -> i.getProductId().equals(r.getProductId())
                    && i.getProductType().equalsIgnoreCase(r.getProductType()));
            if (!exists) {
                throw new BusinessException(400, "评价商品不属于该订单");
            }
        }

        for (OrderItemReviewSubmitRequest r : request.getItems()) {
            Long existing = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                    .eq(Review::getOrderId, order.getId())
                    .eq(Review::getUserId, userId)
                    .eq(Review::getProductType, r.getProductType())
                    .eq(Review::getProductId, r.getProductId())
                    .eq(Review::getReviewType, "ORIGINAL"));
            if (existing != null && existing > 0) {
                throw new BusinessException(400, "该商品已评价，请勿重复提交");
            }
        }

        for (OrderItemReviewSubmitRequest r : request.getItems()) {
            Review review = new Review();
            review.setOrderId(order.getId());
            review.setProductType(r.getProductType());
            review.setProductId(r.getProductId());
            review.setUserId(userId);
            review.setScore(r.getScore());
            review.setContent(r.getContent());
            review.setReviewType("ORIGINAL");
            review.setStatus(1);
            reviewMapper.insert(review);
        }
        order.setOrderStatus(OrderStatusEnum.COMPLETED.getCode());
        order.setCompletedTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);
        return buildOrderVO(order, orderItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO refundMyOrder(Long orderId, OrderRefundApplyRequest request) {
        Long userId = requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        AccessControl.requireOrderOwnedByBuyer(order, "无权操作该订单");
        OrderStateMachine.assertRefundApplyOrderStatusAllowed(order, "当前状态不可退货");
        if (Integer.valueOf(0).equals(order.getCanRefund())) {
            throw new BusinessException(400, "该订单已确认收货，不可申请退款");
        }
        OrderStateMachine.assertRefundActionAllowed(order, OrderStateMachine.RefundAction.APPLY, "当前状态不可重复申请退货");
        String refundMode = (request != null && StringUtils.hasText(request.getRefundMode()))
                ? request.getRefundMode().trim().toUpperCase()
                : "RETURN_REFUND";
        validateRefundMode(order, refundMode);
        LocalDateTime now = LocalDateTime.now();
        Integer version = normalizeVersion(order);
        String reason = (request != null && StringUtils.hasText(request.getReason()))
                ? request.getReason().trim()
                : "买家申请退货";
        String proofUrls = (request != null && request.getProofUrls() != null && !request.getProofUrls().isEmpty())
                ? String.join(",", request.getProofUrls())
                : null;

        int updated = orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                .set("refund_status", RefundStatusEnum.PROCESSING.getCode())
                .set("refund_apply_time", now)
                .set("refund_mode", refundMode)
                .set("refund_reason", reason)
                .set(proofUrls != null, "refund_proof_urls", proofUrls)
                .setSql("version = version + 1")
                .eq("id", orderId)
                .eq("buyer_user_id", userId)
                .in("refund_status", RefundStatusEnum.NONE.getCode(), RefundStatusEnum.REJECTED.getCode())
                .ne("order_status", OrderStatusEnum.PENDING_PAY.getCode())
                .ne("order_status", OrderStatusEnum.CLOSED.getCode())
                .eq("version", version));
        if (updated == 0) {
            throw new BusinessException(400, "当前状态不可重复申请退货");
        }
        if ("ONLY_REFUND".equals(refundMode)) {
            // 仅退款：待发货时直接回流买家个人账户并关闭订单
            escrowSettlementService.changePersonalBalance(order.getBuyerUserId(), resolveRefundAmount(order), orderId,
                    "REFUND_ONLY", TransactionTradeTypeEnum.REFUND_BACKFLOW, "仅退款回流");
            orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                    .set("refund_status", RefundStatusEnum.APPROVED.getCode())
                    .set("order_status", OrderStatusEnum.CLOSED.getCode())
                    .set("closed_time", now)
                    .set("can_refund", 0)
                    .eq("id", orderId));
            restoreConsumedVoucherOnRefundIfNeeded(order);
            pushOrderRealtime(orderId, userId, resolveSellerUserIds(orderId), "AFTER_SALE_UPDATED", "仅退款已自动完成");
            return getMyOrderDetail(orderId);
        }
        insertAfterSaleLog(orderId, AfterSaleActionEnum.APPLY, userId, OperatorRoleEnum.BUYER, reason);
        pushOrderRealtime(orderId, userId, resolveSellerUserIds(orderId), "AFTER_SALE_UPDATED", "买家发起了退货申请");
        return getMyOrderDetail(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO approveRefundBySeller(Long orderId) {
        Long sellerUserId = requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        AccessControl.requireSellerOwnership(
                () -> items.stream().anyMatch(item -> isItemOwnedBySeller(item, sellerUserId)), "无权操作该订单");
        OrderStateMachine.assertRefundActionAllowed(order, OrderStateMachine.RefundAction.APPROVE, "当前无可处理退货申请");
        LocalDateTime now = LocalDateTime.now();
        Integer version = normalizeVersion(order);
        String decisionRemark = StringUtils.hasText(order.getRefundDecisionRemark())
                ? order.getRefundDecisionRemark().trim()
                : "卖家同意退货";
        int updated = orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                .set("refund_status", RefundStatusEnum.APPROVED.getCode())
                .set("order_status", OrderStatusEnum.CLOSED.getCode())
                .set("refund_decision_time", now)
                .set("refund_decision_user_id", sellerUserId)
                .set("refund_decision_remark", decisionRemark)
                .set("refund_decision_source", RefundDecisionSourceEnum.SELLER.name())
                .set("closed_time", now)
            .set("can_refund", 0)
                .setSql("version = version + 1")
                .eq("id", orderId)
                .eq("refund_status", RefundStatusEnum.PROCESSING.getCode())
                .eq("version", version));
        if (updated == 0) {
            throw new BusinessException(400, "当前无可处理退货申请");
        }
        escrowSettlementService.changePersonalBalance(order.getBuyerUserId(), resolveRefundAmount(order), orderId,
                "REFUND_RETURN", TransactionTradeTypeEnum.REFUND_BACKFLOW, "退货退款回流");
        restoreConsumedVoucherOnRefundIfNeeded(order);
        // 条件更新不会回写内存对象，这里同步补齐返回 VO 需要的字段
        order.setRefundStatus(RefundStatusEnum.APPROVED.getCode());
        order.setOrderStatus(OrderStatusEnum.CLOSED.getCode());
        order.setRefundDecisionTime(now);
        order.setRefundDecisionUserId(sellerUserId);
        order.setRefundDecisionRemark(decisionRemark);
        order.setRefundDecisionSource(RefundDecisionSourceEnum.SELLER.name());
        order.setClosedTime(now);
        order.setCanRefund(0);
        insertAfterSaleLog(orderId, AfterSaleActionEnum.APPROVE, sellerUserId, OperatorRoleEnum.SELLER, decisionRemark);
        pushOrderRealtime(orderId, order.getBuyerUserId(), List.of(sellerUserId), "AFTER_SALE_UPDATED", "卖家已同意退货并退款");
        return buildOrderVO(order, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO approveRefundByAdmin(Long orderId, Long adminUserId, String remark) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        OrderStateMachine.assertRefundActionAllowed(order, OrderStateMachine.RefundAction.APPROVE, "当前无可处理的退款申请");

        LocalDateTime now = LocalDateTime.now();
        Integer version = normalizeVersion(order);
        String decisionRemark = StringUtils.hasText(remark) ? remark.trim() : "同意退货";
        int updated = orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                .set("refund_status", RefundStatusEnum.APPROVED.getCode())
                .set("order_status", OrderStatusEnum.CLOSED.getCode())
                .set("refund_decision_time", now)
                .set("refund_decision_user_id", adminUserId)
                .set("refund_decision_remark", decisionRemark)
                .set("refund_decision_source", RefundDecisionSourceEnum.ADMIN.name())
                .set("closed_time", now)
                .set("can_refund", 0)
                .setSql("version = version + 1")
                .eq("id", orderId)
                .eq("refund_status", RefundStatusEnum.PROCESSING.getCode())
                .eq("version", version));
        if (updated == 0) {
            throw new BusinessException(400, "当前无可处理的退款申请");
        }

        escrowSettlementService.changePersonalBalance(order.getBuyerUserId(), resolveRefundAmount(order), orderId,
                "REFUND_RETURN", TransactionTradeTypeEnum.REFUND_BACKFLOW, decisionRemark);
        restoreConsumedVoucherOnRefundIfNeeded(order);

        order.setRefundStatus(RefundStatusEnum.APPROVED.getCode());
        order.setOrderStatus(OrderStatusEnum.CLOSED.getCode());
        order.setRefundDecisionTime(now);
        order.setRefundDecisionUserId(adminUserId);
        order.setRefundDecisionRemark(decisionRemark);
        order.setRefundDecisionSource(RefundDecisionSourceEnum.ADMIN.name());
        order.setClosedTime(now);
        order.setCanRefund(0);

        insertAfterSaleLog(orderId, AfterSaleActionEnum.APPROVE, adminUserId, OperatorRoleEnum.ADMIN, decisionRemark);
        pushOrderRealtime(orderId, order.getBuyerUserId(), resolveSellerUserIds(orderId), "AFTER_SALE_UPDATED", "管理员已同意退货并退款");

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        return buildOrderVO(order, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO rejectRefundBySeller(Long orderId) {
        Long sellerUserId = requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        AccessControl.requireSellerOwnership(
                () -> items.stream().anyMatch(item -> isItemOwnedBySeller(item, sellerUserId)), "无权操作该订单");
        OrderStateMachine.assertRefundActionAllowed(order, OrderStateMachine.RefundAction.REJECT, "当前无可处理退货申请");
        LocalDateTime now = LocalDateTime.now();
        Integer version = normalizeVersion(order);
        String decisionRemark = StringUtils.hasText(order.getRefundDecisionRemark())
                ? order.getRefundDecisionRemark().trim()
                : "卖家拒绝退货";
        int updated = orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                .set("refund_status", RefundStatusEnum.REJECTED.getCode())
                .set("refund_decision_time", now)
                .set("refund_decision_user_id", sellerUserId)
                .set("refund_decision_remark", decisionRemark)
                .set("refund_decision_source", RefundDecisionSourceEnum.SELLER.name())
                .setSql("version = version + 1")
                .eq("id", orderId)
                .eq("refund_status", RefundStatusEnum.PROCESSING.getCode())
                .eq("version", version));
        if (updated == 0) {
            throw new BusinessException(400, "当前无可处理退货申请");
        }
        order.setRefundStatus(RefundStatusEnum.REJECTED.getCode());
        order.setRefundDecisionTime(now);
        order.setRefundDecisionUserId(sellerUserId);
        order.setRefundDecisionRemark(decisionRemark);
        order.setRefundDecisionSource(RefundDecisionSourceEnum.SELLER.name());
        insertAfterSaleLog(orderId, AfterSaleActionEnum.REJECT, sellerUserId, OperatorRoleEnum.SELLER, decisionRemark);
        pushOrderRealtime(orderId, order.getBuyerUserId(), List.of(sellerUserId), "AFTER_SALE_UPDATED", "卖家已拒绝退货申请");
        return buildOrderVO(order, items);
    }

    @Override
    public PageVO<OrderVO> pageSellerOrders(OrderPageQueryRequest request) {
        Long sellerUserId = requireUserId();
        Page<OrderInfo> page = orderInfoMapper.selectPage(Page.of(request.getPageNum(), request.getPageSize()),
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(request.getOrderStatus() != null, OrderInfo::getOrderStatus, request.getOrderStatus())
                        .eq(request.getRefundStatus() != null, OrderInfo::getRefundStatus, request.getRefundStatus())
                        .orderByDesc(OrderInfo::getCreateTime));
        List<OrderVO> records = page.getRecords().stream().map(order -> {
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId())
                    .orderByAsc(OrderItem::getId));
            if (items.isEmpty()) {
                return null;
            }
            boolean hasSellerItem = items.stream().anyMatch(item -> isItemOwnedBySeller(item, sellerUserId));
            if (!hasSellerItem) {
                return null;
            }
            if (StringUtils.hasText(request.getKeyword())) {
                String kw = request.getKeyword().trim();
                boolean hitOrderNo = order.getOrderNo() != null && order.getOrderNo().contains(kw);
                boolean hitProduct = items.stream()
                        .anyMatch(i -> i.getProductName() != null && i.getProductName().contains(kw));
                if (!hitOrderNo && !hitProduct) {
                    return null;
                }
            }
            return buildOrderVO(order, items);
        }).filter(Objects::nonNull).toList();

        PageVO<OrderVO> vo = new PageVO<>();
        vo.setTotal((long) records.size());
        vo.setPageNum(page.getCurrent());
        vo.setPageSize(page.getSize());
        vo.setRecords(records);
        return vo;
    }

    @Override
    public OrderVO getSellerOrderDetail(Long orderId) {
        Long sellerUserId = requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        if (items.isEmpty()) {
            throw new BusinessException(404, "订单不存在");
        }
        AccessControl.requireSellerOwnership(
                () -> items.stream().anyMatch(item -> isItemOwnedBySeller(item, sellerUserId)), "无权查看该订单");
        return buildOrderVO(order, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO shipSellerOrder(Long orderId) {
        Long sellerUserId = requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!Integer.valueOf(OrderStatusEnum.PENDING_SHIP.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException(400, "仅待发货订单可发货");
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        AccessControl.requireSellerOwnership(
                () -> items.stream().anyMatch(item -> isItemOwnedBySeller(item, sellerUserId)), "无权操作该订单");
        order.setOrderStatus(OrderStatusEnum.SHIPPED.getCode());
        LocalDateTime now = LocalDateTime.now();
        order.setShippedTime(now);
        order.setDeliveryTime(now);
        order.setLogisticsStatus("IN_TRANSIT");
        if (order.getLogisticsCurrentIndex() == null) {
            order.setLogisticsCurrentIndex(0);
        }
        orderInfoMapper.updateById(order);
        logisticsService.initializeWhenShipped(orderId);
        pushOrderRealtime(orderId, order.getBuyerUserId(), List.of(sellerUserId), "ORDER_STATUS_UPDATED", "卖家已发货");
        pushOrderRealtime(orderId, order.getBuyerUserId(), List.of(sellerUserId), "LOGISTICS_UPDATED",
                "物流状态更新：包裹已揽收，开始运输");
        return buildOrderVO(order, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remindShipMyOrder(Long orderId) {
        requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        AccessControl.requireOrderOwnedByBuyer(order, "无权操作该订单");
        if (!Integer.valueOf(OrderStatusEnum.PENDING_SHIP.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException(400, "仅待发货订单可提醒发货");
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        if (items.isEmpty()) {
            throw new BusinessException(400, "订单商品为空");
        }

        Long sellerUserId = null;
        OrderItem first = items.get(0);
        if ("NEW".equalsIgnoreCase(first.getProductType())) {
            Product product = productMapper.selectById(first.getProductId());
            if (product != null) {
                Shop shop = shopMapper.selectById(product.getShopId());
                if (shop != null) {
                    sellerUserId = shop.getOwnerUserId();
                }
            }
        } else if ("SECONDHAND".equalsIgnoreCase(first.getProductType())) {
            SecondhandProduct secondhand = secondhandProductMapper.selectById(first.getProductId());
            if (secondhand != null) {
                sellerUserId = secondhand.getSellerUserId();
            }
        }
        if (sellerUserId == null) {
            throw new BusinessException(400, "无法定位卖家，提醒失败");
        }

        Notification n = new Notification();
        n.setUserId(sellerUserId);
        n.setTitle("买家提醒发货");
        n.setContent("订单号：" + order.getOrderNo() + "，请尽快发货。");
        n.setIsRead(0);
        notificationService.createNotification(sellerUserId, n.getTitle(), n.getContent());
        pushOrderRealtime(orderId, order.getBuyerUserId(), List.of(sellerUserId), "ORDER_REMIND_SHIP", "买家已发起提醒发货");
    }

    private Map<Long, Integer> mergeItems(List<CreateOrderItemRequest> items) {
        Map<Long, Integer> merged = new LinkedHashMap<>();
        for (CreateOrderItemRequest item : items) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            merged.merge(item.getProductId(), item.getQuantity(), (a, b) -> a + b);
        }
        return merged;
    }

    private String generateOrderNo(Long userId) {
        String timePart = LocalDateTime.now().format(ORDER_NO_FORMATTER);
        return "ORD" + timePart + String.format("%04d", userId % 10000);
    }

    private Long requireUserId() {
        return AccessControl.requireUserId();
    }

    private Integer normalizeVersion(OrderInfo order) {
        return order != null && order.getVersion() != null ? order.getVersion() : 0;
    }

    private OrderVO buildOrderVO(OrderInfo order, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBuyerUserId(order.getBuyerUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayStatus(order.getPayStatus());
        vo.setOrderStatus(order.getOrderStatus());
        OrderStatusEnum statusEnum = OrderStatusEnum.of(order.getOrderStatus());
        vo.setOrderStatusName(statusEnum == null ? "未知" : statusEnum.getDesc());
        vo.setRefundStatus(order.getRefundStatus() == null ? 0 : order.getRefundStatus());
        vo.setRefundReason(order.getRefundReason());
        vo.setRefundProofUrls(order.getRefundProofUrls());
        vo.setRefundStatusName(toRefundStatusName(order.getRefundStatus()));
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverProvince(order.getReceiverProvince());
        vo.setReceiverCity(order.getReceiverCity());
        vo.setReceiverDetailAddress(order.getReceiverDetailAddress());
        vo.setPayMethod(order.getPayMethod());
        vo.setDeliveryNo(order.getDeliveryNo());
        vo.setRemark(order.getRemark());
        vo.setTradeMode("邮寄");
        vo.setCreateTime(order.getCreateTime());
        vo.setPaidTime(order.getPaidTime());
        vo.setShippedTime(order.getShippedTime());
        vo.setDeliveryTime(order.getDeliveryTime());
        vo.setArrivalTime(order.getArrivalTime());
        vo.setAutoConfirmDeadline(order.getAutoConfirmDeadline());
        vo.setReceivedTime(order.getReceivedTime());
        vo.setCompletedTime(order.getCompletedTime());
        vo.setClosedTime(order.getClosedTime());
        vo.setRefundApplyTime(order.getRefundApplyTime());
        vo.setRefundDecisionTime(order.getRefundDecisionTime());
        vo.setRefundDecisionUserId(order.getRefundDecisionUserId());
        vo.setRefundDecisionRemark(order.getRefundDecisionRemark());
        vo.setRefundDecisionSource(order.getRefundDecisionSource());
        vo.setLogisticsTemplateId(order.getLogisticsTemplateId());
        String normalizedLogisticsStatus = order.getLogisticsStatus();
        if (!StringUtils.hasText(normalizedLogisticsStatus)
                || "PENDING".equalsIgnoreCase(normalizedLogisticsStatus)) {
            if (Integer.valueOf(OrderStatusEnum.SHIPPED.getCode()).equals(order.getOrderStatus())
                    || Integer.valueOf(OrderStatusEnum.RECEIVED.getCode()).equals(order.getOrderStatus())
                    || Integer.valueOf(OrderStatusEnum.COMPLETED.getCode()).equals(order.getOrderStatus())) {
                normalizedLogisticsStatus = "IN_TRANSIT";
            }
        }
        vo.setLogisticsStatus(normalizedLogisticsStatus);
        vo.setLogisticsCurrentIndex(order.getLogisticsCurrentIndex());
        vo.setCanRefund(order.getCanRefund() == null ? 1 : order.getCanRefund());
        vo.setAfterSalesDeadline(order.getAfterSalesDeadline());
        vo.setRefundMode(order.getRefundMode());
        vo.setVoucherId(order.getVoucherId());
        vo.setVoucherDiscountAmount(order.getVoucherDiscountAmount());
        vo.setSellerBearAmount(order.getSellerBearAmount());
        vo.setPlatformBearAmount(order.getPlatformBearAmount());
        vo.setPayableAmount(order.getPayableAmount());
        vo.setItems(items.stream().map(this::toItemVO).toList());
        return vo;
    }

    private void validateRefundMode(OrderInfo order, String refundMode) {
        if ("ONLY_REFUND".equals(refundMode)) {
            if (!Integer.valueOf(OrderStatusEnum.PENDING_SHIP.getCode()).equals(order.getOrderStatus())) {
                throw new BusinessException(400, "仅退款仅支持待发货订单");
            }
            return;
        }
        if ("RETURN_REFUND".equals(refundMode)) {
            boolean shippedOrArrived = Integer.valueOf(OrderStatusEnum.SHIPPED.getCode()).equals(order.getOrderStatus())
                    || "ARRIVED".equalsIgnoreCase(order.getLogisticsStatus());
            boolean inAfterSale = order.getAfterSalesDeadline() != null
                    && LocalDateTime.now().isBefore(order.getAfterSalesDeadline());
            if (!shippedOrArrived && !inAfterSale) {
                throw new BusinessException(400, "退货退款仅支持已发货/已送达或售后保护期内订单");
            }
            return;
        }
        throw new BusinessException(400, "退款模式不支持");
    }

    private void insertAfterSaleLog(Long orderId, AfterSaleActionEnum action, Long operatorUserId,
            OperatorRoleEnum operatorRole, String remark) {
        if (orderAfterSaleLogMapper == null || orderId == null) {
            return;
        }
        String normalizedRemark = StringUtils.hasText(remark) ? remark.trim() : "";
        OrderAfterSaleLog latest = orderAfterSaleLogMapper.selectOne(new LambdaQueryWrapper<OrderAfterSaleLog>()
                .eq(OrderAfterSaleLog::getOrderId, orderId)
                .eq(OrderAfterSaleLog::getAction, action.name())
                .eq(OrderAfterSaleLog::getOperatorUserId, operatorUserId)
                .eq(OrderAfterSaleLog::getOperatorRole, operatorRole.name())
                .orderByDesc(OrderAfterSaleLog::getId)
                .last("limit 1"));
        if (latest != null
                && normalizedRemark.equals(StringUtils.hasText(latest.getRemark()) ? latest.getRemark().trim() : "")) {
            return;
        }
        OrderAfterSaleLog log = new OrderAfterSaleLog();
        log.setOrderId(orderId);
        log.setAction(action.name());
        log.setOperatorUserId(operatorUserId);
        log.setOperatorRole(operatorRole.name());
        if (StringUtils.hasText(normalizedRemark)) {
            log.setRemark(normalizedRemark);
        }
        log.setCreateTime(LocalDateTime.now());
        orderAfterSaleLogMapper.insert(log);
    }

    private OrderItemVO toItemVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(item.getId());
        vo.setProductType(item.getProductType());
        vo.setProductId(item.getProductId());
        vo.setProductName(item.getProductName());
        vo.setPrice(item.getPrice());
        vo.setQuantity(item.getQuantity());
        if ("SECONDHAND".equalsIgnoreCase(item.getProductType())) {
            SecondhandProduct secondhand = secondhandProductMapper.selectById(item.getProductId());
            if (secondhand != null) {
                vo.setConditionLevel(secondhand.getConditionLevel());
                vo.setSellerUserId(secondhand.getSellerUserId());
                User seller = secondhand.getSellerUserId() == null ? null : userMapper.selectById(secondhand.getSellerUserId());
                if (seller != null) {
                    vo.setSellerName(StringUtils.hasText(seller.getNickname()) ? seller.getNickname() : seller.getUsername());
                }
            }
        } else {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                Shop shop = shopMapper.selectById(product.getShopId());
                if (shop != null) {
                    vo.setSellerUserId(shop.getOwnerUserId());
                    User seller = shop.getOwnerUserId() == null ? null : userMapper.selectById(shop.getOwnerUserId());
                    if (seller != null) {
                        vo.setSellerName(StringUtils.hasText(seller.getNickname()) ? seller.getNickname() : seller.getUsername());
                    } else {
                        vo.setSellerName(shop.getName());
                    }
                }
            }
        }
        return vo;
    }

    private boolean isItemOwnedBySeller(OrderItem item, Long sellerUserId) {
        if (item == null || item.getProductType() == null || item.getProductId() == null) {
            return false;
        }
        if ("NEW".equalsIgnoreCase(item.getProductType())) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                return false;
            }
            Shop shop = shopMapper.selectById(product.getShopId());
            return shop != null && sellerUserId.equals(shop.getOwnerUserId());
        }
        if ("SECONDHAND".equalsIgnoreCase(item.getProductType())) {
            SecondhandProduct secondhand = secondhandProductMapper.selectById(item.getProductId());
            return secondhand != null && sellerUserId.equals(secondhand.getSellerUserId());
        }
        return false;
    }

    private String toRefundStatusName(Integer refundStatus) {
        RefundStatusEnum statusEnum = RefundStatusEnum.of(refundStatus);
        return statusEnum == null ? "未知" : statusEnum.getDesc();
    }

    private List<Long> resolveSellerUserIds(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        Set<Long> sellerIds = new HashSet<>();
        for (OrderItem item : items) {
            if ("NEW".equalsIgnoreCase(item.getProductType())) {
                Product product = productMapper.selectById(item.getProductId());
                if (product != null) {
                    Shop shop = shopMapper.selectById(product.getShopId());
                    if (shop != null && shop.getOwnerUserId() != null) {
                        sellerIds.add(shop.getOwnerUserId());
                    }
                }
            } else if ("SECONDHAND".equalsIgnoreCase(item.getProductType())) {
                SecondhandProduct secondhand = secondhandProductMapper.selectById(item.getProductId());
                if (secondhand != null && secondhand.getSellerUserId() != null) {
                    sellerIds.add(secondhand.getSellerUserId());
                }
            }
        }
        return new ArrayList<>(sellerIds);
    }

    private void pushOrderRealtime(Long orderId, Long buyerUserId, List<Long> sellerUserIds, String eventType,
            String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId);
        payload.put("message", message);
        createOrderNotification(buyerUserId, buildBuyerNotificationTitle(eventType), orderId, message);
        for (Long sellerUserId : sellerUserIds) {
            createOrderNotification(sellerUserId, buildSellerNotificationTitle(eventType), orderId, message);
        }
        realtimePushService.pushToUser(buyerUserId, eventType, payload);
        realtimePushService.pushToUsers(sellerUserIds, eventType, payload);
    }

    private void createOrderNotification(Long userId, String title, Long orderId, String message) {
        if (userId == null || !StringUtils.hasText(title) || !StringUtils.hasText(message)) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent("订单#" + orderId + "：" + message.trim());
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        notificationService.createNotification(userId, title, notification.getContent());
    }

    private String buildBuyerNotificationTitle(String eventType) {
        return switch (String.valueOf(eventType).toUpperCase()) {
            case "LOGISTICS_UPDATED" -> "物流通知";
            case "AFTER_SALE_UPDATED" -> "售后通知";
            case "ORDER_REMIND_SHIP" -> "订单通知";
            default -> "订单通知";
        };
    }

    private String buildSellerNotificationTitle(String eventType) {
        return switch (String.valueOf(eventType).toUpperCase()) {
            case "AFTER_SALE_UPDATED" -> "售后提醒";
            case "ORDER_REMIND_SHIP" -> "催发货提醒";
            case "LOGISTICS_UPDATED" -> "物流同步";
            default -> "店铺订单提醒";
        };
    }

    private void distributeVoucherCostBeforeSettlement(OrderInfo order, List<OrderItem> items) {
        BigDecimal sellerBear = order.getSellerBearAmount() == null ? BigDecimal.ZERO : order.getSellerBearAmount();
        if (sellerBear.compareTo(BigDecimal.ZERO) <= 0 || items == null || items.isEmpty()) {
            return;
        }

        Long issuerUserId = null;
        if (order.getVoucherId() != null) {
            Voucher voucher = voucherMapper.selectById(order.getVoucherId());
            if (voucher != null) {
                issuerUserId = voucher.getIssuerUserId();
            }
        }
        if (issuerUserId == null) {
            return;
        }
        final Long finalIssuerUserId = issuerUserId;

        boolean issuerInOrder = items.stream().anyMatch(item -> finalIssuerUserId.equals(resolveItemSellerUserId(item)));
        if (!issuerInOrder) {
            return;
        }

        escrowSettlementService.changeBusinessBalance(
                issuerUserId,
                sellerBear.negate(),
                order.getId(),
                "VOUCHER_SUBSIDY",
                TransactionTradeTypeEnum.EXPENSE_PURCHASE,
                "卖家券补贴扣减");
    }

    private Long resolveItemSellerUserId(OrderItem item) {
        if (item == null || item.getProductType() == null || item.getProductId() == null) {
            return null;
        }
        if ("NEW".equalsIgnoreCase(item.getProductType())) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                return null;
            }
            Shop shop = shopMapper.selectById(product.getShopId());
            return shop == null ? null : shop.getOwnerUserId();
        }
        if ("SECONDHAND".equalsIgnoreCase(item.getProductType())) {
            SecondhandProduct secondhand = secondhandProductMapper.selectById(item.getProductId());
            return secondhand == null ? null : secondhand.getSellerUserId();
        }
        return null;
    }

    private void applyVoucherIfNeeded(OrderInfo order, Long userId, BigDecimal totalAmount, Long voucherId) {
        order.setVoucherDiscountAmount(BigDecimal.ZERO);
        order.setSellerBearAmount(BigDecimal.ZERO);
        order.setPlatformBearAmount(BigDecimal.ZERO);
        order.setPayableAmount(totalAmount);

        if (voucherId == null) {
            return;
        }

        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new BusinessException(400, "优惠券不存在");
        }
        if (voucher.getStatus() == null || voucher.getStatus() != 1) {
            throw new BusinessException(400, "优惠券当前不可用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartTime() != null && now.isBefore(voucher.getStartTime())) {
            throw new BusinessException(400, "优惠券未到生效时间");
        }
        if (voucher.getEndTime() != null && now.isAfter(voucher.getEndTime())) {
            throw new BusinessException(400, "优惠券已过期");
        }

        UserVoucher userVoucher = userVoucherMapper.selectOne(new LambdaQueryWrapper<UserVoucher>()
                .eq(UserVoucher::getUserId, userId)
                .eq(UserVoucher::getVoucherId, voucherId)
                .eq(UserVoucher::getStatus, 1)
                .last("limit 1"));
        if (userVoucher == null) {
            throw new BusinessException(400, "您未持有该优惠券或已使用");
        }

        BigDecimal minAmount = voucher.getMinAmount() == null ? BigDecimal.ZERO : voucher.getMinAmount();
        if (totalAmount.compareTo(minAmount) < 0) {
            throw new BusinessException(400, "未达到优惠券使用门槛");
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (Integer.valueOf(1).equals(voucher.getType())) {
            discount = voucher.getDiscountAmount() == null ? BigDecimal.ZERO : voucher.getDiscountAmount();
        } else if (Integer.valueOf(2).equals(voucher.getType())) {
            BigDecimal rate = voucher.getDiscountRate() == null ? BigDecimal.ONE : voucher.getDiscountRate();
            discount = totalAmount.multiply(BigDecimal.ONE.subtract(rate));
        }
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "优惠券金额无效");
        }
        BigDecimal maxDiscount = totalAmount.subtract(new BigDecimal("0.01"));
        if (discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }

        order.setVoucherDiscountAmount(discount);
        order.setPayableAmount(totalAmount.subtract(discount));

        boolean adminVoucher = Integer.valueOf(2).equals(voucher.getIssuerType()) || Integer.valueOf(2).equals(voucher.getVoucherType());
        if (adminVoucher) {
            order.setSellerBearAmount(BigDecimal.ZERO);
            order.setPlatformBearAmount(discount);
        } else {
            order.setSellerBearAmount(discount);
            order.setPlatformBearAmount(BigDecimal.ZERO);
        }

        // 下单时仅做可用性校验，不修改 user_voucher 状态。
        // 订单创建成功后会绑定 usedOrderId，支付成功后再核销为 USED。
    }

    private void bindVoucherToOrderIfNeeded(Long orderId, Long userId, Long voucherId) {
        if (orderId == null || userId == null || voucherId == null) {
            return;
        }
        UserVoucher userVoucher = userVoucherMapper.selectOne(new LambdaQueryWrapper<UserVoucher>()
                .eq(UserVoucher::getUserId, userId)
                .eq(UserVoucher::getVoucherId, voucherId)
                .eq(UserVoucher::getStatus, 1)
                .isNull(UserVoucher::getUsedOrderId)
                .orderByAsc(UserVoucher::getId)
                .last("limit 1"));
        if (userVoucher == null) {
            throw new BusinessException(400, "您未持有该优惠券或已使用");
        }
        userVoucher.setUsedOrderId(orderId);
        userVoucherMapper.updateById(userVoucher);
    }

    private void consumeVoucherOnPayIfNeeded(Long orderId, Long userId) {
        OrderInfo latest = orderInfoMapper.selectById(orderId);
        if (latest == null || latest.getVoucherId() == null) {
            return;
        }

        UserVoucher userVoucher = userVoucherMapper.selectOne(new LambdaQueryWrapper<UserVoucher>()
                .eq(UserVoucher::getUserId, userId)
                .eq(UserVoucher::getVoucherId, latest.getVoucherId())
                .eq(UserVoucher::getStatus, 1)
                .eq(UserVoucher::getUsedOrderId, orderId)
                .orderByDesc(UserVoucher::getId)
                .last("limit 1"));
        if (userVoucher == null) {
            throw new BusinessException(400, "优惠券状态异常，请刷新后重试");
        }
        userVoucher.setStatus(2);
        userVoucher.setUsedTime(LocalDateTime.now());
        userVoucherMapper.updateById(userVoucher);

        Voucher voucher = voucherMapper.selectById(latest.getVoucherId());
        if (voucher != null) {
            voucher.setUsedCount((voucher.getUsedCount() == null ? 0 : voucher.getUsedCount()) + 1);
            voucherMapper.updateById(voucher);
        }
    }

    private void rollbackVoucherIfNeeded(Long orderId, Long userId) {
        OrderInfo latest = orderInfoMapper.selectById(orderId);
        if (latest == null || latest.getVoucherId() == null) {
            return;
        }

        UserVoucher userVoucher = userVoucherMapper.selectOne(new LambdaQueryWrapper<UserVoucher>()
                .eq(UserVoucher::getUserId, userId)
                .eq(UserVoucher::getVoucherId, latest.getVoucherId())
                .eq(UserVoucher::getStatus, 1)
                .eq(UserVoucher::getUsedOrderId, orderId)
                .orderByDesc(UserVoucher::getId)
                .last("limit 1"));
        if (userVoucher != null) {
            userVoucher.setUsedOrderId(null);
            userVoucherMapper.updateById(userVoucher);
        }
    }

    private BigDecimal resolveRefundAmount(OrderInfo order) {
        if (order == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal refundAmount = order.getPayableAmount();
        if (refundAmount == null) {
            BigDecimal total = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
            BigDecimal discount = order.getVoucherDiscountAmount() == null ? BigDecimal.ZERO : order.getVoucherDiscountAmount();
            refundAmount = total.subtract(discount);
        }
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return refundAmount;
    }

    private void restoreConsumedVoucherOnRefundIfNeeded(OrderInfo order) {
        if (order == null || order.getVoucherId() == null || order.getBuyerUserId() == null) {
            return;
        }
        UserVoucher usedVoucher = userVoucherMapper.selectOne(new LambdaQueryWrapper<UserVoucher>()
                .eq(UserVoucher::getUserId, order.getBuyerUserId())
                .eq(UserVoucher::getVoucherId, order.getVoucherId())
                .eq(UserVoucher::getStatus, 2)
                .eq(UserVoucher::getUsedOrderId, order.getId())
                .orderByDesc(UserVoucher::getId)
                .last("limit 1"));
        if (usedVoucher == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = voucherMapper.selectById(order.getVoucherId());
        LocalDateTime voucherEndTime = voucher == null ? null : voucher.getEndTime();
        boolean expired = (voucherEndTime != null && now.isAfter(voucherEndTime))
                || (usedVoucher.getExpireTime() != null && now.isAfter(usedVoucher.getExpireTime()));

        usedVoucher.setStatus(expired ? 3 : 1);
        usedVoucher.setUsedOrderId(null);
        usedVoucher.setUsedTime(null);
        if (usedVoucher.getExpireTime() == null && voucherEndTime != null) {
            usedVoucher.setExpireTime(voucherEndTime);
        }
        userVoucherMapper.updateById(usedVoucher);

        if (voucher != null && voucher.getUsedCount() != null && voucher.getUsedCount() > 0) {
            voucher.setUsedCount(voucher.getUsedCount() - 1);
            voucherMapper.updateById(voucher);
        }
    }

    private void restoreStockForNewItems(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            String productType = item.getProductType();
            if (StringUtils.hasText(productType) && !"NEW".equalsIgnoreCase(productType.trim())) {
                continue;
            }
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                continue;
            }
            int currentStock = product.getStock() == null ? 0 : product.getStock();
            int restoreQty = item.getQuantity() == null ? 0 : item.getQuantity();
            int nextStock = currentStock + restoreQty;
            product.setStock(nextStock);
            if (nextStock > 0 && Integer.valueOf(ProductStatusEnum.OFF_SHELF.getCode()).equals(product.getStatus())) {
                product.setStatus(ProductStatusEnum.ON_SHELF.getCode());
            }
            productMapper.updateById(product);
        }
    }
}

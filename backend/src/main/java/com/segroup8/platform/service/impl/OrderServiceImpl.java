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
import com.segroup8.platform.dto.OrderShipRequest;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.entity.Review;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.entity.Address;
import com.segroup8.platform.entity.OrderAfterSaleLog;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.OrderAfterSaleLogMapper;
import com.segroup8.platform.mapper.ReviewMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.AddressMapper;
import com.segroup8.platform.mapper.UserBlockMapper;
import com.segroup8.platform.realtime.RealtimePushService;
import com.segroup8.platform.service.OrderService;
import com.segroup8.platform.service.LogisticsService;
import com.segroup8.platform.service.NotificationService;
import com.segroup8.platform.service.VoucherService;
import com.segroup8.platform.service.settlement.EscrowSettlementService;
import com.segroup8.platform.vo.OrderItemVO;
import com.segroup8.platform.vo.OrderVO;
import com.segroup8.platform.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ReviewMapper reviewMapper;
    private final SecondhandProductMapper secondhandProductMapper;
    private final ShopMapper shopMapper;
    private final AddressMapper addressMapper;
    private final UserBlockMapper userBlockMapper;
    private final OrderAfterSaleLogMapper orderAfterSaleLogMapper;
    private final RealtimePushService realtimePushService;
    private final LogisticsService logisticsService;
    private final NotificationService notificationService;
    private final EscrowSettlementService escrowSettlementService;
    private final VoucherService voucherService;

    public OrderServiceImpl(OrderInfoMapper orderInfoMapper, OrderItemMapper orderItemMapper,
            ProductMapper productMapper, ReviewMapper reviewMapper, SecondhandProductMapper secondhandProductMapper,
            ShopMapper shopMapper,
            AddressMapper addressMapper,
            UserBlockMapper userBlockMapper,
            OrderAfterSaleLogMapper orderAfterSaleLogMapper,
            RealtimePushService realtimePushService, LogisticsService logisticsService,
            EscrowSettlementService escrowSettlementService, VoucherService voucherService,
            NotificationService notificationService) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.reviewMapper = reviewMapper;
        this.secondhandProductMapper = secondhandProductMapper;
        this.shopMapper = shopMapper;
        this.addressMapper = addressMapper;
        this.userBlockMapper = userBlockMapper;
        this.orderAfterSaleLogMapper = orderAfterSaleLogMapper;
        this.realtimePushService = realtimePushService;
        this.logisticsService = logisticsService;
        this.escrowSettlementService = escrowSettlementService;
        this.voucherService = voucherService;
        this.notificationService = notificationService;
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
        Map<Long, BigDecimal> shopAmounts = new LinkedHashMap<>();
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
            assertNewProductPurchaseAllowed(userId, product);

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
            if (product.getShopId() != null) {
                shopAmounts.merge(product.getShopId(), itemAmount, BigDecimal::add);
            }

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
        order.setVoucherDiscountAmount(BigDecimal.ZERO);
        order.setSellerBearAmount(BigDecimal.ZERO);
        order.setPlatformBearAmount(BigDecimal.ZERO);
        order.setPayableAmount(totalAmount);
        order.setPayStatus(0);
        order.setOrderStatus(OrderStatusEnum.PENDING_PAY.getCode());
        order.setCanRefund(1);
        order.setLogisticsStatus("PENDING");
        order.setLogisticsCurrentIndex(0);
        order.setPayMethod("余额支付");
        order.setRemark(request.getRemark());
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

        if (request.getVoucherId() != null) {
            VoucherService.CheckoutDiscount discount = voucherService.occupyForOrder(
                    request.getVoucherId(), userId, order.getId(), shopAmounts, totalAmount);
            order.setVoucherId(discount.voucherId());
            order.setVoucherDiscountAmount(discount.discountAmount());
            order.setSellerBearAmount(discount.sellerBearAmount());
            order.setPlatformBearAmount(discount.platformBearAmount());
            order.setPayableAmount(discount.payableAmount());
            orderInfoMapper.updateById(order);
        }

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
            String productType = normalizeProductType(request.getProductType());
            if (productType != null) {
                items = filterItemsByProductType(items, productType);
                if (items.isEmpty()) {
                    return null;
                }
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
    public OrderVO getMyOrderDetail(Long orderId) {
        Long userId = requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        boolean isBuyer = Objects.equals(userId, order.getBuyerUserId());
        boolean isSeller = items.stream().anyMatch(item -> isItemOwnedBySeller(item, userId));
        if (!isBuyer && !isSeller) {
            throw new BusinessException(403, "无权查看该订单");
        }
        return buildOrderVO(order, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO payMyOrder(Long orderId, PayOrderRequest request) {
        Long userId = requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        AccessControl.requireOrderOwnedByBuyer(order, "无权操作该订单");
        OrderStateMachine.assertOrderActionAllowed(order, OrderStateMachine.OrderAction.PAY, "当前状态不可支付");
        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
        if (orderItems.isEmpty()) {
            throw new BusinessException(400, "订单商品不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        String payMode = request != null && StringUtils.hasText(request.getPayMode())
                ? request.getPayMode().trim().toUpperCase()
                : "THIRD_PARTY";
        String payChannel = request != null && StringUtils.hasText(request.getPayChannel())
                ? request.getPayChannel().trim().toUpperCase()
                : "WECHAT";
        String payMethod;
        if ("COIN".equals(payMode)) {
            escrowSettlementService.changePersonalBalance(userId,
                    payableAmount(order).negate(),
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
        voucherService.markUsedForPaidOrder(order.getVoucherId(), userId, orderId);
        order.setPayStatus(1);
        order.setPaidTime(now);
        order.setPayMethod(payMethod);
        order.setOrderStatus(OrderStatusEnum.PENDING_SHIP.getCode());
        order.setVersion(version + 1);

        List<PaidOrderPart> paidOrders = splitPaidOrderByItem(order, orderItems);
        for (PaidOrderPart part : paidOrders) {
            List<Long> sellerUserIds = resolveSellerUserIds(part.items());
            notifySellers(part.order().getId(), sellerUserIds, "新订单已支付",
                    "订单号：" + part.order().getOrderNo() + "，买家已完成支付，请及时发货。");
            pushOrderRealtime(part.order().getId(), userId, sellerUserIds,
                    "ORDER_STATUS_UPDATED", "订单已支付，等待卖家发货");
        }
        PaidOrderPart firstPart = paidOrders.get(0);
        return buildOrderVO(firstPart.order(), firstPart.items());
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
            restoreItemsForUnpaidCancellation(orderId);
            voucherService.releaseForCanceledOrder(order.getVoucherId(), userId, orderId);
        }
        List<Long> sellerUserIds = resolveSellerUserIds(orderId);
        notifySellers(orderId, sellerUserIds, "订单已取消",
                "订单号：" + order.getOrderNo() + "，买家已取消订单。");
        pushOrderRealtime(orderId, userId, sellerUserIds, "ORDER_STATUS_UPDATED", "订单已取消");
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
        List<Long> sellerUserIds = resolveSellerUserIds(orderId);
        notifySellers(orderId, sellerUserIds, "买家已确认收货",
                "订单号：" + order.getOrderNo() + "，交易资金已按规则结算。");
        pushOrderRealtime(orderId, userId, sellerUserIds, "ORDER_STATUS_UPDATED", "买家已确认收货");
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
        List<Long> sellerUserIds = resolveSellerUserIds(orderId);
        notifyBuyer(order, "订单已自动确认收货",
                "订单号：" + order.getOrderNo() + " 已由系统自动确认收货。");
        notifySellers(orderId, sellerUserIds, "订单已自动确认收货",
                "订单号：" + order.getOrderNo() + " 已自动确认收货，交易资金已按规则结算。");
        pushOrderRealtime(orderId, order.getBuyerUserId(), sellerUserIds, "ORDER_STATUS_UPDATED",
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
        refundMoneyToBuyer(order, orderId, "REFUND_TIMEOUT_AUTO", decisionRemark);
        insertAfterSaleLog(orderId, AfterSaleActionEnum.APPROVE, 0L, OperatorRoleEnum.ADMIN, decisionRemark);
        List<Long> sellerUserIds = resolveSellerUserIds(orderId);
        notifyBuyer(order, "退款已完成", "订单号：" + order.getOrderNo() + "，系统已自动退款。");
        notifySellers(orderId, sellerUserIds, "订单已自动退款",
                "订单号：" + order.getOrderNo() + "，因超时未处理，系统已自动退款。");
        pushOrderRealtime(orderId, order.getBuyerUserId(), sellerUserIds, "AFTER_SALE_UPDATED",
                "系统已自动退款");
    }

    private void finalizeReceipt(Long orderId, OrderInfo order, LocalDateTime now) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
        escrowSettlementService.releaseEscrow(order, items);
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
        List<Long> sellerUserIds = resolveSellerUserIds(orderId);
        notifySellers(orderId, sellerUserIds, "订单已完成",
                "订单号：" + order.getOrderNo() + "，买家已完成订单。");
        pushOrderRealtime(orderId, userId, sellerUserIds, "ORDER_STATUS_UPDATED", "订单已完成");
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
        List<Long> sellerUserIds = resolveSellerUserIds(orderId);
        notifySellers(orderId, sellerUserIds, "收到新的订单评价",
                "订单号：" + order.getOrderNo() + "，买家已提交评价。");
        pushOrderRealtime(orderId, userId, sellerUserIds, "ORDER_STATUS_UPDATED", "买家已提交评价，订单已完成");
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
        List<Long> sellerUserIds = resolveSellerUserIds(orderId);
        notifySellers(orderId, sellerUserIds, "收到新的商品评价",
                "订单号：" + order.getOrderNo() + "，买家已提交商品评价。");
        pushOrderRealtime(orderId, userId, sellerUserIds, "ORDER_STATUS_UPDATED", "买家已提交评价，订单已完成");
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
        if ("ONLY_REFUND".equals(refundMode)
                && Integer.valueOf(OrderStatusEnum.PENDING_SHIP.getCode()).equals(order.getOrderStatus())) {
            // 仅退款：待发货时直接回流买家个人账户并关闭订单
            refundMoneyToBuyer(order, orderId, "REFUND_ONLY", "仅退款回流");
            orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                    .set("refund_status", RefundStatusEnum.APPROVED.getCode())
                    .set("order_status", OrderStatusEnum.CLOSED.getCode())
                    .set("refund_decision_time", now)
                    .set("refund_decision_user_id", 0L)
                    .set("refund_decision_remark", "未发货，仅退款自动通过")
                    .set("refund_decision_source", RefundDecisionSourceEnum.SYSTEM.name())
                    .set("closed_time", now)
                    .set("can_refund", 0)
                    .eq("id", orderId));
            restoreStockForNewItems(orderId);
            insertAfterSaleLog(orderId, AfterSaleActionEnum.APPLY, userId, OperatorRoleEnum.BUYER, reason);
            insertAfterSaleLog(orderId, AfterSaleActionEnum.APPROVE, 0L, OperatorRoleEnum.ADMIN, "未发货，仅退款自动通过");
            List<Long> sellerUserIds = resolveSellerUserIds(orderId);
            notifyBuyer(order, "退款已完成", "订单号：" + order.getOrderNo() + "，未发货订单已自动退款。");
            notifySellers(orderId, sellerUserIds, "订单已退款",
                    "订单号：" + order.getOrderNo() + "，买家申请未发货退款，系统已自动处理。");
            pushOrderRealtime(orderId, userId, sellerUserIds, "AFTER_SALE_UPDATED", "仅退款已自动完成");
            return getMyOrderDetail(orderId);
        }
        insertAfterSaleLog(orderId, AfterSaleActionEnum.APPLY, userId, OperatorRoleEnum.BUYER, reason);
        List<Long> sellerUserIds = resolveSellerUserIds(orderId);
        notifySellers(orderId, sellerUserIds, "收到退款申请",
                "订单号：" + order.getOrderNo() + "，买家申请退款，请及时处理。原因：" + reason);
        pushOrderRealtime(orderId, userId, sellerUserIds, "AFTER_SALE_UPDATED", "买家发起了退货申请");
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
        refundMoneyToBuyer(order, orderId, "REFUND_RETURN", "退货退款回流");
        if ("ONLY_REFUND".equalsIgnoreCase(order.getRefundMode())
                && Integer.valueOf(OrderStatusEnum.SHIPPED.getCode()).equals(order.getOrderStatus())
                && !"ARRIVED".equalsIgnoreCase(order.getLogisticsStatus())) {
            notifySellers(orderId, resolveSellerUserIds(orderId), "快递拦截提醒",
                    "订单号：" + order.getOrderNo() + "，您已同意仅退款，请尽快联系物流拦截快递。");
        }
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
        notifyBuyer(order, "退款申请已通过",
                "订单号：" + order.getOrderNo() + "，卖家已同意退款。处理说明：" + decisionRemark);
        pushOrderRealtime(orderId, order.getBuyerUserId(), List.of(sellerUserId), "AFTER_SALE_UPDATED", "卖家已同意退货并退款");
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
        notifyBuyer(order, "退款申请未通过",
                "订单号：" + order.getOrderNo() + "，卖家拒绝了退款申请。处理说明：" + decisionRemark);
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
            String productType = normalizeProductType(request.getProductType());
            if (productType != null) {
                items = filterItemsByProductType(items, productType);
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
    public OrderVO shipSellerOrder(Long orderId, OrderShipRequest request) {
        Long sellerUserId = requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        if (items.size() > 1) {
            throw new BusinessException(409, "合并订单不能直接发货，请先完成订单拆分");
        }
        AccessControl.requireSellerOwnership(
                () -> items.stream().anyMatch(item -> isItemOwnedBySeller(item, sellerUserId)), "无权操作该订单");
        if (Integer.valueOf(OrderStatusEnum.SHIPPED.getCode()).equals(order.getOrderStatus())) {
            logisticsService.initializeWhenShipped(orderId, request);
            return buildOrderVO(order, items);
        }
        if (!Integer.valueOf(OrderStatusEnum.PENDING_SHIP.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException(400, "仅待发货订单可发货");
        }
        order.setOrderStatus(OrderStatusEnum.SHIPPED.getCode());
        LocalDateTime now = LocalDateTime.now();
        order.setShippedTime(now);
        order.setDeliveryTime(now);
        order.setLogisticsStatus("IN_TRANSIT");
        if (order.getLogisticsCurrentIndex() == null) {
            order.setLogisticsCurrentIndex(0);
        }
        orderInfoMapper.updateById(order);
        OrderShipRequest effectiveRequest = normalizeShipRequest(request, items);
        logisticsService.initializeWhenShipped(orderId, effectiveRequest);
        notifyBuyer(order, "订单已发货",
                "订单号：" + order.getOrderNo() + "，卖家已发货，包裹正在运输中。");
        pushOrderRealtime(orderId, order.getBuyerUserId(), List.of(sellerUserId), "ORDER_STATUS_UPDATED", "卖家已发货");
        pushOrderRealtime(orderId, order.getBuyerUserId(), List.of(sellerUserId), "LOGISTICS_UPDATED",
                "物流状态更新：包裹已揽收，开始运输");
        return buildOrderVO(order, items);
    }

    public OrderVO shipSellerOrder(Long orderId) {
        return shipSellerOrder(orderId, null);
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

        notifySellers(orderId, List.of(sellerUserId), "买家提醒发货",
                "订单号：" + order.getOrderNo() + "，请尽快发货。");
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

    private void assertNewProductPurchaseAllowed(Long buyerUserId, Product product) {
        if (buyerUserId == null || product == null || product.getShopId() == null) {
            return;
        }
        Shop shop = shopMapper.selectById(product.getShopId());
        Long sellerUserId = shop == null ? null : shop.getOwnerUserId();
        if (sellerUserId == null) {
            return;
        }
        if (userBlockMapper.isBlocked(buyerUserId, sellerUserId) > 0) {
            throw new BusinessException(403, "您已拉黑该卖家，无法购买其商品");
        }
        if (userBlockMapper.isBlocked(sellerUserId, buyerUserId) > 0) {
            throw new BusinessException(403, "该卖家已拉黑您，无法购买其商品");
        }
    }

    private OrderShipRequest normalizeShipRequest(OrderShipRequest request, List<OrderItem> items) {
        boolean hasSecondhandItem = items != null && items.stream()
                .anyMatch(item -> "SECONDHAND".equalsIgnoreCase(item.getProductType()));
        if (!hasSecondhandItem) {
            return request;
        }
        if (request != null && StringUtils.hasText(request.getOriginProvince())) {
            return request;
        }
        throw new BusinessException(400, "二手订单发货前请填写发货省份");
    }

    private String generateOrderNo(Long userId) {
        String timePart = LocalDateTime.now().format(ORDER_NO_FORMATTER);
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD" + timePart + String.format("%04d", userId % 10000) + randomPart;
    }

    /**
     * A cart checkout is paid once, then converted to one independently fulfillable
     * order per product line. The original order keeps the first line so existing
     * payment and voucher references remain valid; the remaining lines receive new
     * order records and keep their own logistics/after-sale lifecycle.
     */
    private List<PaidOrderPart> splitPaidOrderByItem(OrderInfo paidOrder, List<OrderItem> items) {
        if (items.size() == 1) {
            return List.of(new PaidOrderPart(paidOrder, List.of(items.get(0))));
        }

        List<BigDecimal> grossAmounts = items.stream()
                .map(this::itemAmount)
                .toList();
        BigDecimal voucherDiscount = defaultMoney(paidOrder.getVoucherDiscountAmount());
        BigDecimal sellerBear = defaultMoney(paidOrder.getSellerBearAmount()).min(voucherDiscount);
        BigDecimal platformBear = voucherDiscount.subtract(sellerBear).max(BigDecimal.ZERO);

        Long sellerVoucherShopId = sellerBear.compareTo(BigDecimal.ZERO) > 0
                ? voucherService.getSellerBearerShopId(paidOrder.getVoucherId())
                : null;
        if (sellerBear.compareTo(BigDecimal.ZERO) > 0 && sellerVoucherShopId == null) {
            platformBear = platformBear.add(sellerBear);
            sellerBear = BigDecimal.ZERO;
        }
        List<BigDecimal> sellerWeights = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            if (sellerVoucherShopId == null) {
                sellerWeights.add(BigDecimal.ZERO);
            } else {
                Long shopId = resolveNewProductShopId(items.get(i));
                sellerWeights.add(sellerVoucherShopId.equals(shopId) ? grossAmounts.get(i) : BigDecimal.ZERO);
            }
        }
        List<BigDecimal> sellerAllocations = allocateMoney(sellerBear, sellerWeights);
        List<BigDecimal> platformAllocations = allocateMoney(platformBear, grossAmounts);

        List<PaidOrderPart> result = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            BigDecimal gross = grossAmounts.get(i);
            BigDecimal itemSellerBear = sellerAllocations.get(i);
            BigDecimal itemPlatformBear = platformAllocations.get(i);
            BigDecimal itemDiscount = itemSellerBear.add(itemPlatformBear);
            BigDecimal itemPayable = gross.subtract(itemDiscount).max(BigDecimal.ZERO);

            if (i == 0) {
                applySplitAmounts(paidOrder, gross, itemDiscount, itemSellerBear, itemPlatformBear, itemPayable);
                orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                        .set("total_amount", gross)
                        .set("voucher_discount_amount", itemDiscount)
                        .set("seller_bear_amount", itemSellerBear)
                        .set("platform_bear_amount", itemPlatformBear)
                        .set("payable_amount", itemPayable)
                        .eq("id", paidOrder.getId()));
                result.add(new PaidOrderPart(paidOrder, List.of(item)));
                continue;
            }

            OrderInfo childOrder = copyPaidOrderForSplit(paidOrder, gross, itemDiscount,
                    itemSellerBear, itemPlatformBear, itemPayable);
            orderInfoMapper.insert(childOrder);
            if (childOrder.getId() == null) {
                throw new BusinessException(500, "订单拆分失败：未生成子订单ID");
            }
            item.setOrderId(childOrder.getId());
            if (orderItemMapper.updateById(item) == 0) {
                throw new BusinessException(500, "订单拆分失败：商品明细迁移失败");
            }
            result.add(new PaidOrderPart(childOrder, List.of(item)));
        }
        return result;
    }

    private OrderInfo copyPaidOrderForSplit(OrderInfo source, BigDecimal totalAmount,
            BigDecimal voucherDiscount, BigDecimal sellerBear, BigDecimal platformBear, BigDecimal payable) {
        OrderInfo child = new OrderInfo();
        child.setOrderNo(generateOrderNo(source.getBuyerUserId()));
        child.setBuyerUserId(source.getBuyerUserId());
        child.setPayStatus(1);
        child.setOrderStatus(OrderStatusEnum.PENDING_SHIP.getCode());
        child.setRefundStatus(0);
        child.setReceiverName(source.getReceiverName());
        child.setReceiverPhone(source.getReceiverPhone());
        child.setReceiverProvince(source.getReceiverProvince());
        child.setReceiverCity(source.getReceiverCity());
        child.setReceiverDetailAddress(source.getReceiverDetailAddress());
        child.setPayMethod(source.getPayMethod());
        child.setRemark(source.getRemark());
        child.setCreateTime(source.getCreateTime());
        child.setPaidTime(source.getPaidTime());
        child.setCanRefund(source.getCanRefund() == null ? 1 : source.getCanRefund());
        child.setLogisticsStatus("PENDING");
        child.setLogisticsCurrentIndex(0);
        child.setVersion(0);
        if (voucherDiscount.compareTo(BigDecimal.ZERO) > 0) {
            child.setVoucherId(source.getVoucherId());
        }
        applySplitAmounts(child, totalAmount, voucherDiscount, sellerBear, platformBear, payable);
        return child;
    }

    private void applySplitAmounts(OrderInfo order, BigDecimal totalAmount, BigDecimal voucherDiscount,
            BigDecimal sellerBear, BigDecimal platformBear, BigDecimal payable) {
        order.setTotalAmount(totalAmount);
        order.setVoucherDiscountAmount(voucherDiscount);
        order.setSellerBearAmount(sellerBear);
        order.setPlatformBearAmount(platformBear);
        order.setPayableAmount(payable);
    }

    private List<BigDecimal> allocateMoney(BigDecimal amount, List<BigDecimal> weights) {
        List<BigDecimal> allocations = new ArrayList<>();
        for (int i = 0; i < weights.size(); i++) {
            allocations.add(BigDecimal.ZERO.setScale(2));
        }
        BigDecimal normalizedAmount = defaultMoney(amount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalWeight = weights.stream().map(this::defaultMoney)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0 || totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return allocations;
        }

        List<Integer> eligibleIndexes = new ArrayList<>();
        for (int i = 0; i < weights.size(); i++) {
            if (defaultMoney(weights.get(i)).compareTo(BigDecimal.ZERO) > 0) {
                eligibleIndexes.add(i);
            }
        }
        BigDecimal remaining = normalizedAmount;
        for (int position = 0; position < eligibleIndexes.size(); position++) {
            int index = eligibleIndexes.get(position);
            BigDecimal allocation = position == eligibleIndexes.size() - 1
                    ? remaining
                    : normalizedAmount.multiply(weights.get(index))
                            .divide(totalWeight, 2, RoundingMode.DOWN);
            allocations.set(index, allocation);
            remaining = remaining.subtract(allocation);
        }
        return allocations;
    }

    private BigDecimal itemAmount(OrderItem item) {
        if (item == null || item.getPrice() == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Long resolveNewProductShopId(OrderItem item) {
        if (item == null || !"NEW".equalsIgnoreCase(item.getProductType()) || item.getProductId() == null) {
            return null;
        }
        Product product = productMapper.selectById(item.getProductId());
        return product == null ? null : product.getShopId();
    }

    private record PaidOrderPart(OrderInfo order, List<OrderItem> items) {
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
        vo.setVoucherId(order.getVoucherId());
        vo.setVoucherDiscountAmount(defaultMoney(order.getVoucherDiscountAmount()));
        vo.setSellerBearAmount(defaultMoney(order.getSellerBearAmount()));
        vo.setPlatformBearAmount(defaultMoney(order.getPlatformBearAmount()));
        vo.setPayableAmount(payableAmount(order));
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
        vo.setItems(items.stream().map(this::toItemVO).toList());
        return vo;
    }

    private void validateRefundMode(OrderInfo order, String refundMode) {
        if ("ONLY_REFUND".equals(refundMode)) {
            Integer status = order.getOrderStatus();
            boolean allowed = Integer.valueOf(OrderStatusEnum.PENDING_SHIP.getCode()).equals(status)
                    || Integer.valueOf(OrderStatusEnum.SHIPPED.getCode()).equals(status)
                    || Integer.valueOf(OrderStatusEnum.RECEIVED.getCode()).equals(status)
                    || Integer.valueOf(OrderStatusEnum.COMPLETED.getCode()).equals(status);
            if (!allowed) {
                throw new BusinessException(400, "当前订单不支持仅退款");
            }
            return;
        }
        if ("RETURN_REFUND".equals(refundMode)) {
            boolean arrivedButNotConfirmed = Integer.valueOf(OrderStatusEnum.SHIPPED.getCode()).equals(order.getOrderStatus())
                    && "ARRIVED".equalsIgnoreCase(order.getLogisticsStatus());
            boolean inAfterSale = order.getAfterSalesDeadline() != null
                    && LocalDateTime.now().isBefore(order.getAfterSalesDeadline());
            boolean receivedOrCompleted = Integer.valueOf(OrderStatusEnum.RECEIVED.getCode()).equals(order.getOrderStatus())
                    || Integer.valueOf(OrderStatusEnum.COMPLETED.getCode()).equals(order.getOrderStatus());
            if (!arrivedButNotConfirmed && !(receivedOrCompleted && inAfterSale)) {
                throw new BusinessException(400, "收到货物后才可以申请退货退款");
            }
            return;
        }
        throw new BusinessException(400, "退款方式不支持");
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

    private String normalizeProductType(String productType) {
        if (!StringUtils.hasText(productType)) {
            return null;
        }
        String normalized = productType.trim().toUpperCase();
        if ("ALL".equals(normalized)) {
            return null;
        }
        if ("SECONDHAND".equals(normalized)) {
            return "SECONDHAND";
        }
        if ("NEW".equals(normalized) || "PRODUCT".equals(normalized)) {
            return "NEW";
        }
        return null;
    }

    private List<OrderItem> filterItemsByProductType(List<OrderItem> items, String productType) {
        if (!StringUtils.hasText(productType)) {
            return items;
        }
        return items.stream()
                .filter(item -> productType.equalsIgnoreCase(
                        StringUtils.hasText(item.getProductType()) ? item.getProductType().trim() : "NEW"))
                .toList();
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
                vo.setSellerUserId(secondhand.getSellerUserId());
                vo.setConditionLevel(secondhand.getConditionLevel());
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
        return resolveSellerUserIds(items);
    }

    private List<Long> resolveSellerUserIds(List<OrderItem> items) {
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
        realtimePushService.pushToUser(buyerUserId, eventType, payload);
        realtimePushService.pushToUsers(sellerUserIds, eventType, payload);
    }

    private void refundMoneyToBuyer(OrderInfo order, Long orderId, String changeType, String remark) {
        deductSellerBalanceIfSettled(order, orderId, remark);
        escrowSettlementService.changePersonalBalance(order.getBuyerUserId(), payableAmount(order), orderId,
                changeType, TransactionTradeTypeEnum.REFUND_BACKFLOW, remark);
    }

    private void deductSellerBalanceIfSettled(OrderInfo order, Long orderId, String remark) {
        if (order == null || order.getReceivedTime() == null) {
            return;
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        Map<String, BigDecimal> groupedAmount = new LinkedHashMap<>();
        for (OrderItem item : items) {
            Long sellerUserId = resolveSellerUserId(item);
            if (sellerUserId == null) {
                continue;
            }
            BigDecimal amount = item.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity()));
            String accountType = "SECONDHAND".equalsIgnoreCase(item.getProductType()) ? "PERSONAL" : "BUSINESS";
            groupedAmount.merge(sellerUserId + "#" + accountType, amount, BigDecimal::add);
        }
        for (Map.Entry<String, BigDecimal> entry : groupedAmount.entrySet()) {
            String[] split = entry.getKey().split("#");
            Long sellerUserId = Long.valueOf(split[0]);
            BigDecimal amount = entry.getValue().negate();
            if ("PERSONAL".equals(split[1])) {
                escrowSettlementService.changePersonalBalance(sellerUserId, amount, orderId,
                        "REFUND_SELLER_DEDUCT", TransactionTradeTypeEnum.REFUND_BACKFLOW, remark);
            } else {
                escrowSettlementService.changeBusinessBalance(sellerUserId, amount, orderId,
                        "REFUND_SELLER_DEDUCT", TransactionTradeTypeEnum.REFUND_BACKFLOW, remark);
            }
        }
    }

    private Long resolveSellerUserId(OrderItem item) {
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

    private void applySellerVoucherDiscount(OrderInfo order, Map<String, BigDecimal> groupedAmount) {
        BigDecimal sellerBearAmount = defaultMoney(order == null ? null : order.getSellerBearAmount());
        if (sellerBearAmount.compareTo(BigDecimal.ZERO) <= 0 || order.getVoucherId() == null) {
            return;
        }
        Long voucherShopId = voucherService.getSellerBearerShopId(order.getVoucherId());
        if (voucherShopId == null) {
            return;
        }
        Shop shop = shopMapper.selectById(voucherShopId);
        if (shop == null || shop.getOwnerUserId() == null) {
            return;
        }
        String key = shop.getOwnerUserId() + "#BUSINESS";
        BigDecimal gross = groupedAmount.get(key);
        if (gross != null) {
            groupedAmount.put(key, gross.subtract(sellerBearAmount).max(BigDecimal.ZERO));
        }
    }

    private BigDecimal payableAmount(OrderInfo order) {
        if (order == null) {
            return BigDecimal.ZERO;
        }
        return order.getPayableAmount() == null ? defaultMoney(order.getTotalAmount()) : defaultMoney(order.getPayableAmount());
    }

    private BigDecimal defaultMoney(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private void notifyBuyer(OrderInfo order, String title, String content) {
        if (order == null || order.getBuyerUserId() == null) {
            return;
        }
        notificationService.createNotification(order.getBuyerUserId(), title, content,
                "/order/" + order.getId());
    }

    private void notifySellers(Long orderId, List<Long> sellerUserIds, String title, String content) {
        if (sellerUserIds == null || sellerUserIds.isEmpty()) {
            return;
        }
        for (Long sellerUserId : sellerUserIds.stream().filter(Objects::nonNull).distinct().toList()) {
            notificationService.createNotification(sellerUserId, title, content,
                    resolveSellerOrderTargetPath(orderId, sellerUserId));
        }
    }

    private String resolveSellerOrderTargetPath(Long orderId, Long sellerUserId) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        boolean ownsSecondhandItem = items.stream()
                .filter(item -> "SECONDHAND".equalsIgnoreCase(item.getProductType()))
                .anyMatch(item -> isItemOwnedBySeller(item, sellerUserId));
        return ownsSecondhandItem ? "/secondhand/orders/" + orderId : "/merchant/orders/" + orderId;
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

    private void restoreItemsForUnpaidCancellation(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            if ("SECONDHAND".equalsIgnoreCase(item.getProductType())) {
                secondhandProductMapper.update(null, new UpdateWrapper<SecondhandProduct>()
                        .set("status", 1)
                        .eq("id", item.getProductId())
                        .eq("status", 3));
            }
        }
        restoreStockForNewItems(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO approveRefundByAdmin(Long orderId, Long adminUserId, String remark) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        OrderStateMachine.assertRefundActionAllowed(order, OrderStateMachine.RefundAction.APPROVE, "当前无可处理退款申请");

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        LocalDateTime now = LocalDateTime.now();
        Integer version = normalizeVersion(order);
        Long operatorId = adminUserId == null ? 0L : adminUserId;
        String decisionRemark = StringUtils.hasText(remark) ? remark.trim() : "管理员同意退款";

        int updated = orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                .set("refund_status", RefundStatusEnum.APPROVED.getCode())
                .set("order_status", OrderStatusEnum.CLOSED.getCode())
                .set("refund_decision_time", now)
                .set("refund_decision_user_id", operatorId)
                .set("refund_decision_remark", decisionRemark)
                .set("refund_decision_source", RefundDecisionSourceEnum.ADMIN.name())
                .set("closed_time", now)
                .set("can_refund", 0)
                .setSql("version = version + 1")
                .eq("id", orderId)
                .eq("refund_status", RefundStatusEnum.PROCESSING.getCode())
                .eq("version", version));
        if (updated == 0) {
            throw new BusinessException(400, "当前无可处理退款申请");
        }

        refundMoneyToBuyer(order, orderId, "REFUND_ADMIN", decisionRemark);

        order.setRefundStatus(RefundStatusEnum.APPROVED.getCode());
        order.setOrderStatus(OrderStatusEnum.CLOSED.getCode());
        order.setRefundDecisionTime(now);
        order.setRefundDecisionUserId(operatorId);
        order.setRefundDecisionRemark(decisionRemark);
        order.setRefundDecisionSource(RefundDecisionSourceEnum.ADMIN.name());
        order.setClosedTime(now);
        order.setCanRefund(0);
        insertAfterSaleLog(orderId, AfterSaleActionEnum.APPROVE, operatorId, OperatorRoleEnum.ADMIN, decisionRemark);
        List<Long> sellerUserIds = resolveSellerUserIds(orderId);
        notifyBuyer(order, "退款申请已通过",
                "订单号：" + order.getOrderNo() + "，管理员已同意退款。处理说明：" + decisionRemark);
        notifySellers(orderId, sellerUserIds, "管理员已处理退款",
                "订单号：" + order.getOrderNo() + "，管理员已同意退款。");
        pushOrderRealtime(orderId, order.getBuyerUserId(), sellerUserIds, "AFTER_SALE_UPDATED",
                "管理员已同意退款");
        return buildOrderVO(order, items);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO rejectRefundByAdmin(Long orderId, Long adminUserId, String remark) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        OrderStateMachine.assertRefundActionAllowed(order, OrderStateMachine.RefundAction.REJECT, "当前无可处理退款申请");

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        LocalDateTime now = LocalDateTime.now();
        Integer version = normalizeVersion(order);
        Long operatorId = adminUserId == null ? 0L : adminUserId;
        String decisionRemark = StringUtils.hasText(remark) ? remark.trim() : "管理员拒绝退款";

        int updated = orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                .set("refund_status", RefundStatusEnum.REJECTED.getCode())
                .set("refund_decision_time", now)
                .set("refund_decision_user_id", operatorId)
                .set("refund_decision_remark", decisionRemark)
                .set("refund_decision_source", RefundDecisionSourceEnum.ADMIN.name())
                .setSql("version = version + 1")
                .eq("id", orderId)
                .eq("refund_status", RefundStatusEnum.PROCESSING.getCode())
                .eq("version", version));
        if (updated == 0) {
            throw new BusinessException(400, "当前无可处理退款申请");
        }

        order.setRefundStatus(RefundStatusEnum.REJECTED.getCode());
        order.setRefundDecisionTime(now);
        order.setRefundDecisionUserId(operatorId);
        order.setRefundDecisionRemark(decisionRemark);
        order.setRefundDecisionSource(RefundDecisionSourceEnum.ADMIN.name());
        insertAfterSaleLog(orderId, AfterSaleActionEnum.REJECT, operatorId, OperatorRoleEnum.ADMIN, decisionRemark);
        List<Long> sellerUserIds = resolveSellerUserIds(orderId);
        notifyBuyer(order, "退款申请未通过",
                "订单号：" + order.getOrderNo() + "，管理员拒绝了退款申请。处理说明：" + decisionRemark);
        notifySellers(orderId, sellerUserIds, "管理员已处理退款",
                "订单号：" + order.getOrderNo() + "，管理员已拒绝退款申请。");
        pushOrderRealtime(orderId, order.getBuyerUserId(), sellerUserIds, "AFTER_SALE_UPDATED",
                "管理员已拒绝退款申请");
        return buildOrderVO(order, items);
    }
}

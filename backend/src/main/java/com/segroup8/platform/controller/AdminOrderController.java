package com.segroup8.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.common.OrderStatusEnum;
import com.segroup8.platform.common.OrderStateMachine;
import com.segroup8.platform.common.AccessControl;
import com.segroup8.platform.common.AfterSaleActionEnum;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.OperatorRoleEnum;
import com.segroup8.platform.common.Result;
import com.segroup8.platform.common.RefundDecisionSourceEnum;
import com.segroup8.platform.common.RefundStatusEnum;
import com.segroup8.platform.dto.AdminBatchCloseOrderRequest;
import com.segroup8.platform.dto.AdminRefundDecisionRequest;
import com.segroup8.platform.dto.OrderPageQueryRequest;
import com.segroup8.platform.entity.OrderAfterSaleLog;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.OrderAfterSaleLogMapper;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.realtime.RealtimePushService;
import com.segroup8.platform.vo.OrderItemVO;
import com.segroup8.platform.vo.OrderVO;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.AdminBatchCloseResultVO;
import com.segroup8.platform.vo.AdminBatchCloseFailItemVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final OrderAfterSaleLogMapper orderAfterSaleLogMapper;
    private final RealtimePushService realtimePushService;
    private final Map<Long, String> adminNameCache = new ConcurrentHashMap<>();

    public AdminOrderController(OrderInfoMapper orderInfoMapper, OrderItemMapper orderItemMapper, UserMapper userMapper,
                                OrderAfterSaleLogMapper orderAfterSaleLogMapper, RealtimePushService realtimePushService) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.userMapper = userMapper;
        this.orderAfterSaleLogMapper = orderAfterSaleLogMapper;
        this.realtimePushService = realtimePushService;
    }

    @Operation(summary = "管理员分页查询订单")
    @GetMapping("/list")
    public Result<PageVO<OrderVO>> page(@Valid @ModelAttribute OrderPageQueryRequest request) {
        AccessControl.requireAdmin(userMapper);
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(request.getOrderStatus() != null, OrderInfo::getOrderStatus, request.getOrderStatus())
                .eq(request.getRefundStatus() != null, OrderInfo::getRefundStatus, request.getRefundStatus())
                .orderByDesc(OrderInfo::getCreateTime);

        if (request.getStartTime() != null) {
            LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getStartTime()), ZoneId.of("Asia/Shanghai"));
            wrapper.ge(OrderInfo::getCreateTime, start);
        }
        if (request.getEndTime() != null) {
            LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getEndTime()), ZoneId.of("Asia/Shanghai"));
            wrapper.le(OrderInfo::getCreateTime, end);
        }
        if (request.getMinAmount() != null) {
            wrapper.ge(OrderInfo::getTotalAmount, BigDecimal.valueOf(request.getMinAmount()));
        }
        if (request.getMaxAmount() != null) {
            wrapper.le(OrderInfo::getTotalAmount, BigDecimal.valueOf(request.getMaxAmount()));
        }
        Page<OrderInfo> page = orderInfoMapper.selectPage(Page.of(request.getPageNum(), request.getPageSize()), wrapper);
        List<OrderVO> records = page.getRecords().stream().map(order -> {
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId())
                    .orderByAsc(OrderItem::getId));
            if (StringUtils.hasText(request.getKeyword())) {
                String kw = request.getKeyword().trim();
                boolean hitOrderNo = order.getOrderNo() != null && order.getOrderNo().contains(kw);
                boolean hitProduct = items.stream().anyMatch(i -> i.getProductName() != null && i.getProductName().contains(kw));
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
        return Result.success(vo);
    }

    @Operation(summary = "管理员获取订单详情")
    @GetMapping("/detail/{orderId}")
    public Result<OrderVO> detail(@PathVariable Long orderId) {
        AccessControl.requireAdmin(userMapper);
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        return Result.success(buildOrderVO(order, items));
    }

    @Operation(summary = "管理员批量关闭订单")
    @PostMapping("/batch-close")
    public Result<AdminBatchCloseResultVO> batchClose(@Valid @RequestBody AdminBatchCloseOrderRequest request) {
        AccessControl.requireAdmin(userMapper);
        AdminBatchCloseResultVO result = new AdminBatchCloseResultVO();
        for (Long orderId : request.getOrderIds()) {
            OrderInfo order = orderInfoMapper.selectById(orderId);
            if (order == null) {
                result.getFailedItems().add(buildBatchCloseFail(orderId, "订单不存在", null));
                continue;
            }
            if (Integer.valueOf(OrderStatusEnum.COMPLETED.getCode()).equals(order.getOrderStatus())
                    || Integer.valueOf(OrderStatusEnum.CLOSED.getCode()).equals(order.getOrderStatus())) {
                result.getFailedItems().add(buildBatchCloseFail(orderId, "订单状态不可关闭", order.getOrderStatus()));
                continue;
            }
            order.setOrderStatus(OrderStatusEnum.CLOSED.getCode());
            orderInfoMapper.updateById(order);
            result.getSuccessIds().add(orderId);
            realtimePushService.pushToUser(order.getBuyerUserId(), "ORDER_STATUS_UPDATED", Map.of(
                    "orderId", orderId,
                    "message", "管理员已关闭订单"
            ));
        }
        return Result.success(result);
    }

    @Operation(summary = "管理员同意退货（退款）")
    @PostMapping("/{orderId}/refund/approve")
    public Result<OrderVO> approveRefund(@PathVariable Long orderId, @Valid @RequestBody(required = false) AdminRefundDecisionRequest request) {
        AccessControl.requireAdmin(userMapper);
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        OrderStateMachine.assertRefundActionAllowed(order, OrderStateMachine.RefundAction.APPROVE, "当前无可处理的退款申请");
        order.setRefundStatus(RefundStatusEnum.APPROVED.getCode());
        order.setOrderStatus(OrderStatusEnum.CLOSED.getCode());
        order.setRefundDecisionTime(LocalDateTime.now());
        Long operatorId = AccessControl.requireUserId();
        order.setRefundDecisionUserId(operatorId);
        if (request != null && StringUtils.hasText(request.getRemark())) {
            order.setRefundDecisionRemark(request.getRemark().trim());
        } else {
            order.setRefundDecisionRemark("同意退货");
        }
        order.setRefundDecisionSource(RefundDecisionSourceEnum.ADMIN.name());
        order.setClosedTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);
        insertAfterSaleLog(orderId, AfterSaleActionEnum.APPROVE, operatorId, OperatorRoleEnum.ADMIN, order.getRefundDecisionRemark());
        realtimePushService.pushToUser(order.getBuyerUserId(), "AFTER_SALE_UPDATED", Map.of(
                "orderId", orderId,
                "message", "管理员已同意退货并退款"
        ));

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        return Result.success(buildOrderVO(order, items));
    }

    @Operation(summary = "管理员拒绝退货（退款）")
    @PostMapping("/{orderId}/refund/reject")
    public Result<OrderVO> rejectRefund(@PathVariable Long orderId, @Valid @RequestBody(required = false) AdminRefundDecisionRequest request) {
        AccessControl.requireAdmin(userMapper);
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        OrderStateMachine.assertRefundActionAllowed(order, OrderStateMachine.RefundAction.REJECT, "当前无可处理的退款申请");
        order.setRefundStatus(RefundStatusEnum.REJECTED.getCode());
        order.setOrderStatus(OrderStatusEnum.CLOSED.getCode());
        order.setRefundDecisionTime(LocalDateTime.now());
        Long operatorId = AccessControl.requireUserId();
        order.setRefundDecisionUserId(operatorId);
        if (request != null && StringUtils.hasText(request.getRemark())) {
            order.setRefundDecisionRemark(request.getRemark().trim());
        } else {
            order.setRefundDecisionRemark("拒绝退货");
        }
        order.setRefundDecisionSource(RefundDecisionSourceEnum.ADMIN.name());
        order.setClosedTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);
        insertAfterSaleLog(orderId, AfterSaleActionEnum.REJECT, operatorId, OperatorRoleEnum.ADMIN, order.getRefundDecisionRemark());
        realtimePushService.pushToUser(order.getBuyerUserId(), "AFTER_SALE_UPDATED", Map.of(
                "orderId", orderId,
                "message", "管理员已拒绝退货申请"
        ));

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        return Result.success(buildOrderVO(order, items));
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
        vo.setReceivedTime(order.getReceivedTime());
        vo.setCompletedTime(order.getCompletedTime());
        vo.setClosedTime(order.getClosedTime());
        vo.setRefundApplyTime(order.getRefundApplyTime());
        vo.setRefundDecisionTime(order.getRefundDecisionTime());
        vo.setRefundDecisionUserId(order.getRefundDecisionUserId());
        if (order.getRefundDecisionUserId() != null) {
            String cached = adminNameCache.get(order.getRefundDecisionUserId());
            if (cached == null) {
                User admin = userMapper.selectById(order.getRefundDecisionUserId());
                if (admin != null) {
                    String displayName = admin.getNickname() != null && !admin.getNickname().isBlank()
                            ? admin.getNickname()
                            : admin.getUsername();
                    cached = displayName;
                    adminNameCache.put(order.getRefundDecisionUserId(), displayName);
                }
            }
            vo.setRefundDecisionUserName(cached);
        }
        vo.setRefundDecisionRemark(order.getRefundDecisionRemark());
        vo.setRefundDecisionSource(order.getRefundDecisionSource());
        vo.setItems(items.stream().map(this::toItemVO).toList());
        return vo;
    }

    @Operation(summary = "管理员查询订单售后操作记录")
    @GetMapping("/{orderId}/after-sale-logs")
    public Result<List<OrderAfterSaleLog>> afterSaleLogs(@PathVariable Long orderId) {
        AccessControl.requireAdmin(userMapper);
        return Result.success(orderAfterSaleLogMapper.selectList(new LambdaQueryWrapper<OrderAfterSaleLog>()
                .eq(OrderAfterSaleLog::getOrderId, orderId)
                .orderByAsc(OrderAfterSaleLog::getCreateTime)
                .orderByAsc(OrderAfterSaleLog::getId)));
    }

    private void insertAfterSaleLog(Long orderId, AfterSaleActionEnum action, Long operatorUserId, OperatorRoleEnum operatorRole, String remark) {
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
        if (latest != null && normalizedRemark.equals(StringUtils.hasText(latest.getRemark()) ? latest.getRemark().trim() : "")) {
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
        return vo;
    }

    private String toRefundStatusName(Integer refundStatus) {
        RefundStatusEnum statusEnum = RefundStatusEnum.of(refundStatus);
        return statusEnum == null ? "未知" : statusEnum.getDesc();
    }

    private AdminBatchCloseFailItemVO buildBatchCloseFail(Long orderId, String reason, Integer status) {
        AdminBatchCloseFailItemVO vo = new AdminBatchCloseFailItemVO();
        vo.setOrderId(orderId);
        vo.setReason(reason);
        vo.setCurrentStatus(status);
        OrderStatusEnum statusEnum = OrderStatusEnum.of(status);
        vo.setCurrentStatusName(statusEnum == null ? "-" : statusEnum.getDesc());
        return vo;
    }
}


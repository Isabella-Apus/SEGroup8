package com.segroup8.platform.common;

import com.segroup8.platform.entity.OrderInfo;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 集中式订单/售后状态机（转移表 + 统一校验入口）
 *
 * 目标：把“允许从哪些状态执行哪些动作”收敛到一处，避免规则散落在各个 service/controller 方法里。
 */
public final class OrderStateMachine {

    private OrderStateMachine() {
    }

    public enum OrderAction {
        PAY,
        CANCEL,
        SHIP,
        CONFIRM_RECEIVE,
        COMPLETE
    }

    public enum RefundAction {
        APPLY,
        APPROVE,
        REJECT
    }

    private static final Map<OrderAction, Set<OrderStatusEnum>> ORDER_ALLOWED_FROM = new EnumMap<>(OrderAction.class);
    private static final Map<RefundAction, Set<RefundStatusEnum>> REFUND_ALLOWED_FROM = new EnumMap<>(RefundAction.class);

    static {
        // 订单主流程
        ORDER_ALLOWED_FROM.put(OrderAction.PAY, EnumSet.of(OrderStatusEnum.PENDING_PAY));
        ORDER_ALLOWED_FROM.put(OrderAction.CANCEL, EnumSet.of(
                OrderStatusEnum.PENDING_PAY,
                OrderStatusEnum.PENDING_SHIP,
                OrderStatusEnum.SHIPPED,
                OrderStatusEnum.RECEIVED
        ));
        ORDER_ALLOWED_FROM.put(OrderAction.SHIP, EnumSet.of(OrderStatusEnum.PENDING_SHIP));
        ORDER_ALLOWED_FROM.put(OrderAction.CONFIRM_RECEIVE, EnumSet.of(OrderStatusEnum.SHIPPED));
        ORDER_ALLOWED_FROM.put(OrderAction.COMPLETE, EnumSet.of(OrderStatusEnum.RECEIVED));

        // 售后流程（refund_status: 0无、1处理中、2已退款、3已拒绝）
        REFUND_ALLOWED_FROM.put(RefundAction.APPLY, Set.of(RefundStatusEnum.NONE, RefundStatusEnum.REJECTED)); // 允许从“无售后/已拒绝”再次申请
        REFUND_ALLOWED_FROM.put(RefundAction.APPROVE, Set.of(RefundStatusEnum.PROCESSING));
        REFUND_ALLOWED_FROM.put(RefundAction.REJECT, Set.of(RefundStatusEnum.PROCESSING));
    }

    public static void assertOrderActionAllowed(OrderInfo order, OrderAction action, String messageWhenDenied) {
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        OrderStatusEnum current = OrderStatusEnum.of(order.getOrderStatus());
        Set<OrderStatusEnum> allowed = ORDER_ALLOWED_FROM.get(action);
        if (current == null || allowed == null || !allowed.contains(current)) {
            throw new BusinessException(400, messageWhenDenied);
        }
    }

    public static void assertRefundActionAllowed(OrderInfo order, RefundAction action, String messageWhenDenied) {
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        RefundStatusEnum currentRefund = RefundStatusEnum.of(order.getRefundStatus());
        Set<RefundStatusEnum> allowed = REFUND_ALLOWED_FROM.get(action);
        if (currentRefund == null || allowed == null || !allowed.contains(currentRefund)) {
            throw new BusinessException(400, messageWhenDenied);
        }
    }

    public static void assertRefundApplyOrderStatusAllowed(OrderInfo order, String messageWhenDenied) {
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        // 业务约束：待付款/已关闭不允许申请售后（避免无支付的退款、已关闭的重复售后）
        Integer os = order.getOrderStatus();
        if (os == null
                || Integer.valueOf(OrderStatusEnum.PENDING_PAY.getCode()).equals(os)
                || Integer.valueOf(OrderStatusEnum.CLOSED.getCode()).equals(os)) {
            throw new BusinessException(400, messageWhenDenied);
        }
    }
}


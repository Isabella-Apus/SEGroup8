package com.segroup8.order;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderState {
    CREATING, PENDING_PAY, PAYMENT_PENDING, PENDING_SHIP, SHIPPED, RECEIVED,
    COMPLETED, CANCEL_PENDING, CANCELLED, REFUND_PENDING, REFUNDED;

    private static final Map<Action, Set<OrderState>> ALLOWED = new EnumMap<>(Action.class);
    static {
        ALLOWED.put(Action.PAY, EnumSet.of(PENDING_PAY));
        ALLOWED.put(Action.CANCEL, EnumSet.of(PENDING_PAY, PENDING_SHIP));
        ALLOWED.put(Action.SHIP, EnumSet.of(PENDING_SHIP));
        ALLOWED.put(Action.CONFIRM_RECEIVE, EnumSet.of(SHIPPED));
        ALLOWED.put(Action.COMPLETE, EnumSet.of(RECEIVED));
        ALLOWED.put(Action.REFUND, EnumSet.of(PENDING_SHIP, SHIPPED, RECEIVED, COMPLETED));
    }

    public enum Action { PAY, CANCEL, SHIP, CONFIRM_RECEIVE, COMPLETE, REFUND }

    public static void require(OrderState current, Action action) {
        if (!ALLOWED.getOrDefault(action, Set.of()).contains(current)) {
            throw new OrderException("INVALID_ORDER_TRANSITION",
                    "Action " + action + " is not allowed from " + current, 409);
        }
    }
}

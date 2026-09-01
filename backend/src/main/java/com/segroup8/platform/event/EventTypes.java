package com.segroup8.platform.event;

import java.util.Set;

public final class EventTypes {
    public static final String NOTIFICATION_REQUESTED = "NotificationRequested.v1";
    public static final String ORDER_STATUS_CHANGED = "OrderStatusChanged.v1";
    public static final String PAYMENT_COMPLETED = "PaymentCompleted.v1";
    public static final String REFUND_COMPLETED = "RefundCompleted.v1";
    public static final String MERCHANT_APPROVED = "MerchantApproved.v1";
    public static final String SECONDHAND_TRADE_SETTLED = "SecondhandTradeSettled.v1";
    public static final String USER_ACCESS_CHANGED = "UserAccessChanged.v1";
    public static final Set<String> ALL = Set.of(NOTIFICATION_REQUESTED, ORDER_STATUS_CHANGED,
            PAYMENT_COMPLETED, REFUND_COMPLETED, MERCHANT_APPROVED,
            SECONDHAND_TRADE_SETTLED, USER_ACCESS_CHANGED);

    private EventTypes() { }
}

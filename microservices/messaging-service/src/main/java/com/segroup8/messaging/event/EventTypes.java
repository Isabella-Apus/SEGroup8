package com.segroup8.messaging.event;

import java.util.Set;

public final class EventTypes {
    public static final String NOTIFICATION_REQUESTED = "NotificationRequested.v1";
    public static final String ORDER_STATUS_CHANGED = "OrderStatusChanged.v1";
    public static final String ORDER_REFUND_STATUS_CHANGED = "OrderRefundStatusChanged.v1";
    public static final String ORDER_SHIPMENT_REMINDED = "OrderShipmentReminded.v1";
    public static final String REVIEW_SUBMITTED = "ReviewSubmitted.v1";
    public static final String REVIEW_FOLLOW_UP_SUBMITTED = "ReviewFollowUpSubmitted.v1";
    public static final String PAYMENT_COMPLETED = "PaymentCompleted.v1";
    public static final String REFUND_COMPLETED = "RefundCompleted.v1";
    public static final String MERCHANT_APPROVED = "MerchantApproved.v1";
    public static final String SECONDHAND_TRADE_SETTLED = "SecondhandTradeSettled.v1";
    public static final String USER_ACCESS_CHANGED = "UserAccessChanged.v1";
    public static final Set<String> ALL = Set.of(NOTIFICATION_REQUESTED, ORDER_STATUS_CHANGED,
            ORDER_REFUND_STATUS_CHANGED, ORDER_SHIPMENT_REMINDED, REVIEW_SUBMITTED,
            REVIEW_FOLLOW_UP_SUBMITTED,
            PAYMENT_COMPLETED, REFUND_COMPLETED, MERCHANT_APPROVED,
            SECONDHAND_TRADE_SETTLED, USER_ACCESS_CHANGED);
    private EventTypes() { }
}

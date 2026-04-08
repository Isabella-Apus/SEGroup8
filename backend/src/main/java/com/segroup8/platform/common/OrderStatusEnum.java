package com.segroup8.platform.common;

public enum OrderStatusEnum {
    PENDING_PAY(0, "待付款"),
    PENDING_SHIP(1, "待发货"),
    SHIPPED(2, "待收货"),
    RECEIVED(3, "待评价"),
    COMPLETED(4, "已完成"),
    CLOSED(9, "已关闭");

    private final int code;
    private final String desc;

    OrderStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static OrderStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatusEnum item : values()) {
            if (item.code == code) {
                return item;
            }
        }
        return null;
    }
}


package com.segroup8.platform.common;

public enum RefundStatusEnum {
    NONE(0, "无"),
    PROCESSING(1, "退款中"),
    APPROVED(2, "已退款"),
    REJECTED(3, "退款被拒绝");

    private final int code;
    private final String desc;

    RefundStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static RefundStatusEnum of(Integer code) {
        if (code == null) {
            return NONE;
        }
        for (RefundStatusEnum item : values()) {
            if (item.code == code) {
                return item;
            }
        }
        return null;
    }
}


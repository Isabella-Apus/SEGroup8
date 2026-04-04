package com.segroup8.platform.common;

public enum ProductStatusEnum {
    OFF_SHELF(0, "已下架"),
    ON_SHELF(1, "在售");

    private final int code;
    private final String desc;

    ProductStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ProductStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (ProductStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}

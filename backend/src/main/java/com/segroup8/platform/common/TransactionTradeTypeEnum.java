package com.segroup8.platform.common;

import com.segroup8.platform.service.settlement.SettlementAccountType;

public enum TransactionTradeTypeEnum {
    INCOME_PERSONAL("INCOME_PERSONAL", "个人账户入账"),
    INCOME_BUSINESS("INCOME_BUSINESS", "经营账户入账"),
    EXPENSE_PURCHASE("EXPENSE_PURCHASE", "消费支出"),
    RECHARGE("RECHARGE", "钱包充值"),
    REFUND_BACKFLOW("REFUND_BACKFLOW", "退款回流"),
    UNKNOWN("UNKNOWN", "未知类型");

    private final String code;
    private final String desc;

    TransactionTradeTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static TransactionTradeTypeEnum of(String code) {
        if (code == null || code.isBlank()) {
            return UNKNOWN;
        }
        for (TransactionTradeTypeEnum item : values()) {
            if (item.code.equalsIgnoreCase(code.trim())) {
                return item;
            }
        }
        return UNKNOWN;
    }

    public static TransactionTradeTypeEnum incomeByAccount(SettlementAccountType accountType) {
        if (accountType == SettlementAccountType.BUSINESS) {
            return INCOME_BUSINESS;
        }
        return INCOME_PERSONAL;
    }
}
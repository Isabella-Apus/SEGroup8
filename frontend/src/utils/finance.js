const TRADE_TYPE_LABELS = {
    INCOME_PERSONAL: "个人账户入账",
    INCOME_BUSINESS: "经营账户入账",
    EXPENSE_PURCHASE: "消费支出",
    RECHARGE: "钱包充值",
    REFUND_BACKFLOW: "退款回流",
    UNKNOWN: "未知类型",
};

export function resolveTradeTypeLabel(record) {
    if (record?.tradeTypeName) {
        return record.tradeTypeName;
    }
    const code = String(record?.tradeType || "").toUpperCase();
    return TRADE_TYPE_LABELS[code] || code || "-";
}

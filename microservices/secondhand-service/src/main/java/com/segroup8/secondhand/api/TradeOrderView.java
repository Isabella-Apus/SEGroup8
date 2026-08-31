package com.segroup8.secondhand.api;

import java.math.BigDecimal;

public record TradeOrderView(String tradeType, String tradeId, String orderBusinessKey,
        long productId, BigDecimal price, String requestStatus, Long orderId, String orderNo,
        String orderStatus, String message) {
}

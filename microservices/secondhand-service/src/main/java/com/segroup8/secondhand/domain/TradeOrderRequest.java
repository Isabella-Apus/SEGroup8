package com.segroup8.secondhand.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeOrderRequest(long id, String tradeType, String tradeId, String orderBusinessKey,
        long productId, long buyerUserId, long sellerUserId, BigDecimal price, Long addressId,
        String remark, String requestStatus, Long orderId, String orderNo, String orderStatus,
        int attempts, String lastError, LocalDateTime nextRetryAt, int version,
        LocalDateTime createTime, LocalDateTime updateTime) {
}

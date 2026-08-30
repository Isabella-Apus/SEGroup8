package com.segroup8.secondhand.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductNegotiation(long id, long productId, long buyerUserId, long sellerUserId,
        Long conversationId, BigDecimal proposedPrice, BigDecimal confirmedPrice, String status,
        LocalDateTime effectiveFrom, LocalDateTime effectiveUntil, Long usedOrderId, int version,
        LocalDateTime createTime, LocalDateTime updateTime) {
}

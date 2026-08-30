package com.segroup8.secondhand.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductAuction(long id, long productId, long sellerUserId, BigDecimal startPrice,
        BigDecimal incrementAmount, BigDecimal currentPrice, Long currentBidderUserId,
        LocalDateTime startTime, LocalDateTime endTime, String status, Long settledOrderId,
        int version, LocalDateTime createTime, LocalDateTime updateTime) {
}

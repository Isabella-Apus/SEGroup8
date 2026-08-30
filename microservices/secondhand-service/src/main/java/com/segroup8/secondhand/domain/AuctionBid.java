package com.segroup8.secondhand.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AuctionBid(long id, long auctionId, long productId, long bidderUserId,
        String bidderNameSnapshot, BigDecimal bidAmount, String status, LocalDateTime createTime) {
}

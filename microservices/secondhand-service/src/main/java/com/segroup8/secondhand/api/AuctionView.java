package com.segroup8.secondhand.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AuctionView(long id, long productId, String productName, long sellerUserId,
        BigDecimal startPrice, BigDecimal incrementAmount, BigDecimal currentPrice,
        Long currentBidderUserId, String currentBidderName, LocalDateTime startTime,
        LocalDateTime endTime, String status, String statusName, Long settledOrderId,
        long bidCount, List<BidView> logs, String orderRequestStatus) {

    public record BidView(long id, long bidderUserId, String bidderName, BigDecimal bidAmount,
            String status, LocalDateTime createTime) {
    }
}

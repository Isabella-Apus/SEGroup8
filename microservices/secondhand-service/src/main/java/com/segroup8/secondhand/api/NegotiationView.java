package com.segroup8.secondhand.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NegotiationView(long id, long productId, long buyerUserId, long sellerUserId,
        BigDecimal proposedPrice, BigDecimal confirmedPrice, String status,
        LocalDateTime effectiveFrom, LocalDateTime effectiveUntil, Long orderId,
        String orderRequestStatus) {
}

package com.segroup8.secondhand.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuctionSettlementScheduler {
    private final TradeApplicationService trades;

    public AuctionSettlementScheduler(TradeApplicationService trades) {
        this.trades = trades;
    }

    @Scheduled(fixedDelayString = "${secondhand.auction.settlement-delay-ms:5000}")
    public void settleExpired() {
        trades.settleDueAuctions(20);
    }
}

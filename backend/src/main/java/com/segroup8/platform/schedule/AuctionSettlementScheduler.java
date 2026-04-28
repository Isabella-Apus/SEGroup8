package com.segroup8.platform.schedule;

import com.segroup8.platform.service.SecondhandTradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuctionSettlementScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuctionSettlementScheduler.class);

    private final SecondhandTradeService secondhandTradeService;

    public AuctionSettlementScheduler(SecondhandTradeService secondhandTradeService) {
        this.secondhandTradeService = secondhandTradeService;
    }

    @Scheduled(cron = "0 */1 * * * ?")
    public void settleExpiredAuctions() {
        try {
            secondhandTradeService.settleExpiredAuctions();
        } catch (Exception ex) {
            log.warn("settle auction failed", ex);
        }
    }
}

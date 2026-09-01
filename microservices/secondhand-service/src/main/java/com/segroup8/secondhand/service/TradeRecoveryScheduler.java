package com.segroup8.secondhand.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TradeRecoveryScheduler {
    private static final Logger log = LoggerFactory.getLogger(TradeRecoveryScheduler.class);
    private final TradeOrderCoordinator coordinator;
    private final int batchSize;

    public TradeRecoveryScheduler(TradeOrderCoordinator coordinator,
            @Value("${secondhand.recovery.batch-size:20}") int batchSize) {
        this.coordinator = coordinator;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${secondhand.recovery.delay-ms:5000}")
    public void recover() {
        TradeOrderCoordinator.RecoverySummary result = coordinator.recoverPending(batchSize);
        if (result.scanned() > 0) {
            log.info("trade recovery cycle scanned={} created={} retrying={} failed={}",
                    result.scanned(), result.created(), result.retrying(), result.failed());
        }
    }
}

package com.segroup8.secondhand.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TradeRecoveryScheduler {
    private final TradeOrderCoordinator coordinator;
    private final int batchSize;

    public TradeRecoveryScheduler(TradeOrderCoordinator coordinator,
            @Value("${secondhand.recovery.batch-size:20}") int batchSize) {
        this.coordinator = coordinator;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${secondhand.recovery.delay-ms:5000}")
    public void recover() {
        coordinator.recoverPending(batchSize);
    }
}

package com.segroup8.messaging.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class AfterCommitExecutor {
    private static final Logger log = LoggerFactory.getLogger(AfterCommitExecutor.class);

    public void run(Runnable action) {
        Runnable isolated = () -> {
            try { action.run(); }
            catch (RuntimeException ex) { log.warn("Post-commit realtime delivery failed: {}", ex.getMessage()); }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            isolated.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { isolated.run(); }
        });
    }
}

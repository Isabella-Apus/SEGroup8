package com.segroup8.platform.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.segroup8.platform.entity.IdempotencyRecord;
import com.segroup8.platform.mapper.IdempotencyRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class IdempotencyCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupJob.class);
    private final IdempotencyRecordMapper idempotencyRecordMapper;

    public IdempotencyCleanupJob(IdempotencyRecordMapper idempotencyRecordMapper) {
        this.idempotencyRecordMapper = idempotencyRecordMapper;
    }

    @Scheduled(fixedDelayString = "${app.idempotency.cleanup-fixed-delay-ms:60000}")
    public void cleanupExpiredRecords() {
        int deleted = idempotencyRecordMapper.delete(new LambdaQueryWrapper<IdempotencyRecord>()
                .lt(IdempotencyRecord::getExpireTime, LocalDateTime.now()));
        if (deleted > 0) {
            log.info("idempotency_cleanup deletedExpiredRecords={}", deleted);
        }
    }
}


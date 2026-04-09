package com.segroup8.platform.job;

import com.segroup8.platform.mapper.IdempotencyRecordMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class IdempotencyCleanupJobTest {

    @Test
    void cleanupExpiredRecords_shouldCallDelete() {
        IdempotencyRecordMapper mapper = Mockito.mock(IdempotencyRecordMapper.class);
        Mockito.when(mapper.delete(ArgumentMatchers.any())).thenReturn(1);
        IdempotencyCleanupJob job = new IdempotencyCleanupJob(mapper);
        job.cleanupExpiredRecords();
        Mockito.verify(mapper).delete(ArgumentMatchers.any());
    }
}


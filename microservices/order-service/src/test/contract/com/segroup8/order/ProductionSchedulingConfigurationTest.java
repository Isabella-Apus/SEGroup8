package com.segroup8.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(properties = "outbox.publisher.enabled=true")
@Tag("CONTRACT")
class ProductionSchedulingConfigurationTest {
    @Autowired ApplicationContext context;

    @Test
    void productionDefaultOutboxDelayCreatesScheduledPublisher() {
        assertThat(context.getBean(OutboxPublisher.class)).isNotNull();
    }
}

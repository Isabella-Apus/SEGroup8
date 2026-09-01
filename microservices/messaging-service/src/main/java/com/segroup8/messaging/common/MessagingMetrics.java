package com.segroup8.messaging.common;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** Small metrics surface for delivery health and operations dashboards. */
@Component
public class MessagingMetrics {
    private final AtomicInteger activeWebSocketConnections = new AtomicInteger();
    private final AtomicInteger eventBacklog = new AtomicInteger();
    private final Counter eventConsumeFailures;
    private final Counter pushFailures;
    private final Counter retryCount;

    public MessagingMetrics(MeterRegistry registry) {
        Gauge.builder("messaging.websocket.connections.active", activeWebSocketConnections, AtomicInteger::get)
                .description("Active messaging WebSocket sessions").register(registry);
        Gauge.builder("messaging.events.backlog", eventBacklog, AtomicInteger::get)
                .description("Inbox and delivery events awaiting processing").register(registry);
        eventConsumeFailures = Counter.builder("messaging.events.consume.failures")
                .description("Event processing failures before retry or DLQ").register(registry);
        pushFailures = Counter.builder("messaging.websocket.push.failures")
                .description("WebSocket delivery failures").register(registry);
        retryCount = Counter.builder("messaging.events.retry.count")
                .description("Scheduled event retry attempts").register(registry);
    }

    public void sessionOpened() { activeWebSocketConnections.incrementAndGet(); }
    public void sessionClosed() { activeWebSocketConnections.updateAndGet(value -> Math.max(0, value - 1)); }
    public void setEventBacklog(int value) { eventBacklog.set(Math.max(0, value)); }
    public void eventConsumeFailed() { eventConsumeFailures.increment(); }
    public void pushFailed() { pushFailures.increment(); }
    public void retried() { retryCount.increment(); }
}

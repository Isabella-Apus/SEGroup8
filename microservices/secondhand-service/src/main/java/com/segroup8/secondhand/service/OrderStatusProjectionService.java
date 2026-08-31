package com.segroup8.secondhand.service;

import com.segroup8.secondhand.domain.SecondhandProduct;
import com.segroup8.secondhand.domain.TradeOrderRequest;
import com.segroup8.secondhand.repository.IdempotencyRepository;
import com.segroup8.secondhand.repository.OutboxRepository;
import com.segroup8.secondhand.repository.ProductRepository;
import com.segroup8.secondhand.repository.TradeOrderRequestRepository;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderStatusProjectionService {
    private static final Logger log = LoggerFactory.getLogger(OrderStatusProjectionService.class);
    private final IdempotencyRepository idempotency;
    private final TradeOrderRequestRepository requests;
    private final ProductRepository products;
    private final OutboxRepository outbox;

    public OrderStatusProjectionService(IdempotencyRepository idempotency,
            TradeOrderRequestRepository requests, ProductRepository products, OutboxRepository outbox) {
        this.idempotency = idempotency;
        this.requests = requests;
        this.products = products;
        this.outbox = outbox;
    }

    @Transactional
    public boolean consume(String eventId, String businessKey, long orderId, String newStatus) {
        if (!idempotency.recordOnce("ORDER_STATUS_EVENT", eventId, String.valueOf(orderId))) {
            log.info("order status event duplicate eventId={} orderBusinessKey={} orderId={}",
                    eventId, businessKey, orderId);
            return false;
        }
        TradeOrderRequest request = requests.findByBusinessKey(businessKey).orElse(null);
        if (request == null || request.orderId() == null || request.orderId() != orderId) {
            outbox.append("ORDER_EVENT", eventId, "SecondhandOrderEventUnmatched.v1",
                    Map.of("eventId", eventId, "orderId", orderId, "orderBusinessKey", businessKey));
            log.warn("order status event unmatched eventId={} orderBusinessKey={} orderId={} newStatus={}",
                    eventId, businessKey, orderId, newStatus);
            return true;
        }
        String normalized = newStatus == null ? "UNKNOWN" : newStatus.toUpperCase();
        if ("CANCELLED".equals(normalized) || "CLOSED".equals(normalized)) {
            requests.markCancelled(request.id(), normalized);
            products.compareAndSetStatus(request.productId(), SecondhandProduct.SOLD, SecondhandProduct.ON_SHELF);
            products.compareAndSetStatus(request.productId(), SecondhandProduct.TRADE_PENDING,
                    SecondhandProduct.ON_SHELF);
        } else {
            requests.updateOrderStatus(request.id(), normalized);
        }
        outbox.append("SECONDHAND_TRADE", request.tradeId(), "SecondhandOrderStatusObserved.v1", Map.of(
                "eventId", eventId, "tradeType", request.tradeType(), "tradeId", request.tradeId(),
                "productId", request.productId(), "orderId", orderId, "newStatus", normalized));
        log.info("order status event consumed eventId={} productId={} tradeType={} tradeId={} "
                        + "orderBusinessKey={} orderId={} newStatus={}",
                eventId, request.productId(), request.tradeType(), request.tradeId(),
                businessKey, orderId, normalized);
        return true;
    }
}

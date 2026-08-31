package com.segroup8.secondhand.service;

import com.segroup8.secondhand.api.TradeOrderView;
import com.segroup8.secondhand.client.OrderGateway;
import com.segroup8.secondhand.client.OrderGateway.OrderReceipt;
import com.segroup8.secondhand.client.OrderContractException;
import com.segroup8.secondhand.domain.SecondhandProduct;
import com.segroup8.secondhand.domain.TradeOrderRequest;
import com.segroup8.secondhand.repository.AuctionRepository;
import com.segroup8.secondhand.repository.IdempotencyRepository;
import com.segroup8.secondhand.repository.NegotiationRepository;
import com.segroup8.secondhand.repository.OutboxRepository;
import com.segroup8.secondhand.repository.ProductRepository;
import com.segroup8.secondhand.repository.TradeOrderRequestRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TradeOrderCoordinator {
    private static final Logger log = LoggerFactory.getLogger(TradeOrderCoordinator.class);
    private final TradeOrderRequestRepository requests;
    private final ProductRepository products;
    private final NegotiationRepository negotiations;
    private final AuctionRepository auctions;
    private final IdempotencyRepository idempotency;
    private final OutboxRepository outbox;
    private final OrderGateway orderGateway;
    private final TransactionTemplate transactions;
    private final int maxAttempts;

    public TradeOrderCoordinator(TradeOrderRequestRepository requests, ProductRepository products,
            NegotiationRepository negotiations, AuctionRepository auctions,
            IdempotencyRepository idempotency, OutboxRepository outbox, OrderGateway orderGateway,
            TransactionTemplate transactions, @Value("${clients.order.max-attempts:3}") int maxAttempts) {
        this.requests = requests;
        this.products = products;
        this.negotiations = negotiations;
        this.auctions = auctions;
        this.idempotency = idempotency;
        this.outbox = outbox;
        this.orderGateway = orderGateway;
        this.transactions = transactions;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public TradeOrderRequest dispatch(long requestId) {
        TradeOrderRequest request = requests.findById(requestId).orElseThrow();
        if ("CREATED".equals(request.requestStatus()) || "FAILED".equals(request.requestStatus())
                || "CANCELLED".equals(request.requestStatus())) {
            return request;
        }
        try {
            OrderReceipt receipt = orderGateway.createSecondhandOrder(request);
            complete(request, receipt);
        } catch (OrderContractException contractFailure) {
            log.error("order contract rejected productId={} tradeType={} tradeId={} orderBusinessKey={}",
                    request.productId(), request.tradeType(), request.tradeId(), request.orderBusinessKey(),
                    contractFailure);
            fail(request, rootMessage(contractFailure));
        } catch (RuntimeException createFailure) {
            log.warn("order create failed productId={} tradeType={} tradeId={} orderBusinessKey={}",
                    request.productId(), request.tradeType(), request.tradeId(), request.orderBusinessKey(), createFailure);
            Optional<OrderReceipt> recovered = lookupAfterUncertainFailure(request);
            if (recovered.isPresent()) {
                complete(request, recovered.get());
            } else {
                failOrRetry(request, rootMessage(createFailure));
            }
        }
        return requests.findById(requestId).orElseThrow();
    }

    public RecoverySummary recoverPending(int limit) {
        var due = requests.findRetryable(limit);
        int created = 0;
        int retrying = 0;
        int failed = 0;
        for (TradeOrderRequest request : due) {
            try {
                TradeOrderRequest result = dispatch(request.id());
                if ("CREATED".equals(result.requestStatus())) created++;
                else if ("FAILED".equals(result.requestStatus())) failed++;
                else retrying++;
            } catch (RuntimeException exception) {
                retrying++;
                log.error("trade recovery failed productId={} tradeType={} tradeId={} orderBusinessKey={}",
                        request.productId(), request.tradeType(), request.tradeId(), request.orderBusinessKey(), exception);
            }
        }
        return new RecoverySummary(due.size(), created, retrying, failed);
    }

    public record RecoverySummary(int scanned, int created, int retrying, int failed) {
    }

    public TradeOrderView toView(TradeOrderRequest request) {
        String message = switch (request.requestStatus()) {
            case "CREATED" -> "订单已创建，请前往订单中心付款";
            case "FAILED" -> "订单创建失败，商品已解除冻结";
            case "CANCELLED" -> "订单已取消";
            default -> "订单创建处理中，请稍后在订单中心查看";
        };
        return new TradeOrderView(request.tradeType(), request.tradeId(), request.orderBusinessKey(),
                request.productId(), request.price(), request.requestStatus(), request.orderId(), request.orderNo(),
                request.orderStatus(), message);
    }

    private Optional<OrderReceipt> lookupAfterUncertainFailure(TradeOrderRequest request) {
        try {
            return orderGateway.findByBusinessKey(request.orderBusinessKey());
        } catch (RuntimeException lookupFailure) {
            log.warn("order lookup failed productId={} tradeType={} tradeId={} orderBusinessKey={}",
                    request.productId(), request.tradeType(), request.tradeId(), request.orderBusinessKey(), lookupFailure);
            return Optional.empty();
        }
    }

    private void complete(TradeOrderRequest request, OrderReceipt receipt) {
        transactions.executeWithoutResult(status -> {
            if (requests.markCreated(request.id(), receipt.orderId(), receipt.orderNo(), receipt.status()) == 0) {
                return;
            }
            products.compareAndSetStatus(request.productId(), SecondhandProduct.TRADE_PENDING, SecondhandProduct.SOLD);
            if ("BARGAIN".equals(request.tradeType())) {
                negotiations.markAccepted(Long.parseLong(request.tradeId()), receipt.orderId());
            } else if ("AUCTION".equals(request.tradeType())) {
                auctions.markFinished(Long.parseLong(request.tradeId()), receipt.orderId());
                idempotency.release("ACTIVE_AUCTION", String.valueOf(request.productId()));
            }
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("tradeType", request.tradeType());
            event.put("tradeId", request.tradeId());
            event.put("productId", request.productId());
            event.put("buyerId", request.buyerUserId());
            event.put("sellerId", request.sellerUserId());
            event.put("price", request.price());
            event.put("orderId", receipt.orderId());
            event.put("orderBusinessKey", request.orderBusinessKey());
            outbox.append("SECONDHAND_TRADE", request.tradeId(), "SecondhandTradeSettled.v1", event);
            outbox.append("SECONDHAND_TRADE", request.tradeId(), "NotificationRequested.v1", Map.of(
                    "recipientIds", java.util.List.of(request.buyerUserId(), request.sellerUserId()),
                    "type", "SECONDHAND_ORDER_CREATED", "dedupeKey", request.orderBusinessKey()));
        });
        log.info("order linked productId={} tradeType={} tradeId={} orderBusinessKey={} orderId={}",
                request.productId(), request.tradeType(), request.tradeId(), request.orderBusinessKey(), receipt.orderId());
    }

    private void failOrRetry(TradeOrderRequest request, String error) {
        int nextAttempt = request.attempts() + 1;
        if (nextAttempt < maxAttempts) {
            requests.markRetry(request.id(), error, LocalDateTime.now().plusSeconds(nextAttempt * 2L));
            return;
        }
        fail(request, error);
    }

    private void fail(TradeOrderRequest request, String error) {
        transactions.executeWithoutResult(status -> {
            if (requests.markFailed(request.id(), error) == 0) {
                return;
            }
            products.compareAndSetStatus(request.productId(), SecondhandProduct.TRADE_PENDING,
                    SecondhandProduct.ON_SHELF);
            if ("BARGAIN".equals(request.tradeType())) {
                negotiations.releaseFailed(Long.parseLong(request.tradeId()));
                idempotency.release("BARGAIN_APPLY", request.productId() + ":" + request.buyerUserId());
            } else if ("AUCTION".equals(request.tradeType())) {
                auctions.markFailedFlow(Long.parseLong(request.tradeId()));
                idempotency.release("ACTIVE_AUCTION", String.valueOf(request.productId()));
            }
            outbox.append("SECONDHAND_TRADE", request.tradeId(), "SecondhandTradeOrderFailed.v1", Map.of(
                    "tradeType", request.tradeType(), "tradeId", request.tradeId(),
                    "productId", request.productId(), "orderBusinessKey", request.orderBusinessKey(),
                    "reason", error));
        });
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}

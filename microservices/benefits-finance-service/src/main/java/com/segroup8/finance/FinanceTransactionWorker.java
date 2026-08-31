package com.segroup8.finance;

import com.segroup8.finance.ApiModels.DebitRequest;
import com.segroup8.finance.ApiModels.PaymentResult;
import com.segroup8.finance.ApiModels.RefundRequest;
import com.segroup8.finance.ApiModels.SettlementRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class FinanceTransactionWorker {
    private static final Logger LOG = LoggerFactory.getLogger(FinanceTransactionWorker.class);
    private final JdbcClient db;
    private final String currency;

    FinanceTransactionWorker(JdbcClient db, @Value("${app.currency}") String currency) {
        this.db = db;
        this.currency = currency;
    }

    @Transactional
    public PaymentResult debit(DebitRequest request) {
        BigDecimal amount = money(request.amount());
        insertRequest(request.paymentRequestId(), "DEBIT", request.orderId(), request.userId(), null, amount, null);
        String transactionId = changeBalance(request.paymentRequestId(), request.orderId(), request.userId(),
                "PERSONAL", "DEBIT", amount.negate(), null, "订单扣款");
        complete(request.paymentRequestId(), transactionId, "PaymentCompleted", request.orderId());
        return completed(request.paymentRequestId());
    }

    @Transactional
    public PaymentResult refund(RefundRequest request) {
        PaymentResult original = FinanceService.queryPaymentForUpdate(db, request.paymentRequestId()).orElseThrow(() ->
                DomainException.notFound("PAYMENT_REQUEST_NOT_FOUND", "资金请求不存在"));
        if (!"DEBIT".equals(original.requestType()) || !"COMPLETED".equals(original.status()))
            throw DomainException.conflict("PAYMENT_NOT_REFUNDABLE", "原支付不存在或尚未完成");
        if (!request.orderId().equals(original.orderId()) || !request.userId().equals(original.userId()))
            throw DomainException.conflict("PAYMENT_REFERENCE_MISMATCH", "退款订单或用户与原支付不一致");
        BigDecimal amount = money(request.amount());
        BigDecimal refunded = db.sql("select coalesce(sum(amount),0) from payment_request where request_type='REFUND' "
                        + "and original_request_id=:id and status='COMPLETED'")
                .param("id", request.paymentRequestId()).query(BigDecimal.class).single();
        if (refunded.add(amount).compareTo(original.amount()) > 0)
            throw DomainException.conflict("REFUND_EXCEEDS_PAYMENT", "累计退款金额不能超过原支付金额");
        insertRequest(request.refundRequestId(), "REFUND", request.orderId(), request.userId(), null, amount,
                request.paymentRequestId());
        String transactionId = changeBalance(request.refundRequestId(), request.orderId(), request.userId(),
                "PERSONAL", "REFUND", amount, original.transactionId(), "订单退款");
        complete(request.refundRequestId(), transactionId, "RefundCompleted", request.orderId());
        return completed(request.refundRequestId());
    }

    @Transactional
    public PaymentResult settlement(SettlementRequest request) {
        String requestId = settlementRequestId(request.orderId(), request.sellerId());
        BigDecimal amount = money(request.amount());
        insertRequest(requestId, "SETTLEMENT", request.orderId(), null, request.sellerId(), amount, null);
        String transactionId = changeBalance(requestId, request.orderId(), request.sellerId(),
                "BUSINESS", "SETTLEMENT", amount, null, "订单结算入账");
        complete(requestId, transactionId, "SettlementCompleted", request.orderId());
        return completed(requestId);
    }

    @Transactional
    public PaymentResult recharge(long userId, String clientRequestId, BigDecimal requestedAmount, String channel) {
        String requestId = "recharge:" + userId + ":" + clientRequestId;
        BigDecimal amount = money(requestedAmount);
        insertRequest(requestId, "RECHARGE", 0L, userId, null, amount, null);
        String transactionId = changeBalance(requestId, null, userId, "PERSONAL", "RECHARGE", amount, null,
                "模拟充值-" + (channel == null || channel.isBlank() ? "WECHAT" : channel));
        complete(requestId, transactionId, "RechargeCompleted", 0L);
        return completed(requestId);
    }

    private void insertRequest(String requestId, String type, long orderId, Long userId, Long sellerId,
            BigDecimal amount, String originalRequestId) {
        db.sql("insert into payment_request(request_id,request_type,order_id,user_id,seller_id,amount,currency,status,"
                        + "original_request_id) values(:id,:type,:orderId,:userId,:sellerId,:amount,:currency,'PROCESSING',:original)")
                .param("id", requestId).param("type", type).param("orderId", orderId).param("userId", userId)
                .param("sellerId", sellerId).param("amount", amount).param("currency", currency)
                .param("original", originalRequestId).update();
    }

    private String changeBalance(String businessRequestId, Long orderId, long userId, String accountType,
            String tradeType, BigDecimal delta, String reversalOf, String remark) {
        ensureBalance(userId);
        BalanceRow current = db.sql("select personal_balance,business_balance,version from balance where user_id=:id for update")
                .param("id", userId).query((rs, row) -> new BalanceRow(rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getInt(3)))
                .single();
        BigDecimal currentAmount = "PERSONAL".equals(accountType) ? current.personal() : current.business();
        BigDecimal target = money(currentAmount.add(delta));
        if (target.compareTo(BigDecimal.ZERO) < 0)
            throw DomainException.conflict("INSUFFICIENT_BALANCE", "账户余额不足，未发生扣款");
        String column = "PERSONAL".equals(accountType) ? "personal_balance" : "business_balance";
        int changed = db.sql("update balance set " + column + "=:target,version=version+1,updated_at=current_timestamp "
                        + "where user_id=:id and version=:version")
                .param("target", target).param("id", userId).param("version", current.version()).update();
        if (changed == 0) throw DomainException.conflict("BALANCE_CONCURRENT_UPDATE", "余额发生并发更新，请查询请求结果");
        String transactionId = UUID.randomUUID().toString();
        db.sql("insert into transaction_record(transaction_id,business_request_id,order_id,user_id,account_type,"
                        + "trade_type,amount,balance_after,currency,reversal_of,remark) values(:transactionId,:requestId,"
                        + ":orderId,:userId,:accountType,:tradeType,:amount,:balanceAfter,:currency,:reversalOf,:remark)")
                .param("transactionId", transactionId).param("requestId", businessRequestId).param("orderId", orderId)
                .param("userId", userId).param("accountType", accountType).param("tradeType", tradeType)
                .param("amount", delta).param("balanceAfter", target).param("currency", currency)
                .param("reversalOf", reversalOf).param("remark", remark).update();
        return transactionId;
    }

    private void ensureBalance(long userId) {
        db.sql("insert into balance(user_id,personal_balance,business_balance,version) values(:id,0,0,0) "
                        + "on duplicate key update user_id=user_id")
                .param("id", userId).update();
    }

    private void complete(String requestId, String transactionId, String eventType, long orderId) {
        db.sql("update payment_request set status='COMPLETED',transaction_id=:transactionId,completed_at=current_timestamp "
                        + "where request_id=:id")
                .param("transactionId", transactionId).param("id", requestId).update();
        String eventId = UUID.randomUUID().toString();
        String payload = "{\"requestId\":\"" + requestId + "\",\"orderId\":" + orderId
                + ",\"transactionId\":\"" + transactionId + "\"}";
        db.sql("insert into outbox_event(event_id,aggregate_type,aggregate_id,event_type,payload,status) "
                        + "values(:eventId,'PAYMENT',:aggregateId,:eventType,:payload,'PENDING')")
                .param("eventId", eventId).param("aggregateId", requestId).param("eventType", eventType)
                .param("payload", payload).update();
    }

    private PaymentResult find(String requestId) {
        return FinanceService.queryPayment(db, requestId).orElseThrow(() ->
                DomainException.notFound("PAYMENT_REQUEST_NOT_FOUND", "资金请求不存在"));
    }

    private PaymentResult completed(String requestId) {
        PaymentResult result = find(requestId);
        String paymentRequestId = "REFUND".equals(result.requestType()) ? result.originalRequestId() : result.requestId();
        String refundRequestId = "REFUND".equals(result.requestType()) ? result.requestId() : "-";
        LOG.info("financial_request_completed requestType={} requestId={} paymentRequestId={} refundRequestId={} "
                        + "orderId={} transactionId={} amount={} currency={}",
                result.requestType(), result.requestId(), paymentRequestId, refundRequestId, result.orderId(),
                result.transactionId(), result.amount(), result.currency());
        return result;
    }

    static String settlementRequestId(long orderId, long sellerId) {
        return "settlement:" + orderId + ":" + sellerId;
    }

    private static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private record BalanceRow(BigDecimal personal, BigDecimal business, int version) {}
}

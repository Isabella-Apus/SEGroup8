package com.segroup8.finance;

import com.segroup8.finance.ApiModels.DebitRequest;
import com.segroup8.finance.ApiModels.PaymentResult;
import com.segroup8.finance.ApiModels.RefundRequest;
import com.segroup8.finance.ApiModels.SettlementRequest;
import com.segroup8.finance.ApiModels.TransactionView;
import com.segroup8.finance.ApiModels.Wallet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
class FinanceService {
    private final JdbcClient db;
    private final FinanceTransactionWorker worker;
    private final String currency;

    FinanceService(JdbcClient db, FinanceTransactionWorker worker, @Value("${app.currency}") String currency) {
        this.db = db;
        this.worker = worker;
        this.currency = currency;
    }

    Wallet wallet(long userId) {
        ensureBalance(userId);
        return db.sql("select personal_balance,business_balance,version from balance where user_id=:id")
                .param("id", userId).query((rs, row) -> new Wallet(rs.getBigDecimal(1), rs.getBigDecimal(2),
                        currency, rs.getInt(3))).single();
    }

    PaymentResult recharge(long userId, String requestId, java.math.BigDecimal amount, String channel) {
        String id = "recharge:" + userId + ":" + requestId;
        Optional<PaymentResult> existing = queryPayment(db, id);
        if (existing.isPresent()) return validateSame(existing.get(), "RECHARGE", amount, userId, 0L, null, null);
        try {
            return worker.recharge(userId, requestId, amount, channel);
        } catch (DataIntegrityViolationException concurrent) {
            return validateSame(queryPayment(db, id).orElseThrow(() -> concurrent),
                    "RECHARGE", amount, userId, 0L, null, null);
        }
    }

    PaymentResult debit(DebitRequest request) {
        Optional<PaymentResult> existing = queryPayment(db, request.paymentRequestId());
        if (existing.isPresent()) return validateSame(existing.get(), "DEBIT", request.amount(), request.userId(),
                request.orderId(), null, null);
        try {
            return worker.debit(request);
        } catch (DataIntegrityViolationException concurrent) {
            return validateSame(queryPayment(db, request.paymentRequestId()).orElseThrow(() -> concurrent),
                    "DEBIT", request.amount(), request.userId(), request.orderId(), null, null);
        }
    }

    PaymentResult refund(RefundRequest request) {
        Optional<PaymentResult> existing = queryPayment(db, request.refundRequestId());
        if (existing.isPresent()) return validateSame(existing.get(), "REFUND", request.amount(), request.userId(),
                request.orderId(), null, request.paymentRequestId());
        try {
            return worker.refund(request);
        } catch (DataIntegrityViolationException concurrent) {
            return validateSame(queryPayment(db, request.refundRequestId()).orElseThrow(() -> concurrent),
                    "REFUND", request.amount(), request.userId(), request.orderId(), null, request.paymentRequestId());
        }
    }

    PaymentResult settlement(SettlementRequest request) {
        String id = FinanceTransactionWorker.settlementRequestId(request.orderId(), request.sellerId());
        Optional<PaymentResult> existing = queryPayment(db, id);
        if (existing.isPresent()) return validateSame(existing.get(), "SETTLEMENT", request.amount(), null,
                request.orderId(), request.sellerId(), null);
        try {
            return worker.settlement(request);
        } catch (DataIntegrityViolationException concurrent) {
            return validateSame(queryPayment(db, id).orElseThrow(() -> concurrent),
                    "SETTLEMENT", request.amount(), null, request.orderId(), request.sellerId(), null);
        }
    }

    PaymentResult payment(String requestId) {
        return queryPayment(db, requestId).orElseThrow(() ->
                DomainException.notFound("PAYMENT_REQUEST_NOT_FOUND", "资金请求不存在"));
    }

    List<TransactionView> records(long userId, String accountType) {
        return db.sql("select * from transaction_record where user_id=:userId and account_type=:accountType "
                        + "order by created_at desc limit 100")
                .param("userId", userId).param("accountType", accountType).query(FinanceService::mapTransaction).list();
    }

    private void ensureBalance(long userId) {
        db.sql("insert into balance(user_id,personal_balance,business_balance,version) values(:id,0,0,0) "
                        + "on duplicate key update user_id=user_id")
                .param("id", userId).update();
    }

    private PaymentResult validateSame(PaymentResult existing, String requestType, java.math.BigDecimal amount,
            Long userId, Long orderId, Long sellerId, String originalRequestId) {
        if (!requestType.equals(existing.requestType())
                || existing.amount().compareTo(amount.setScale(2, java.math.RoundingMode.HALF_UP)) != 0
                || (userId != null && !userId.equals(existing.userId()))
                || (orderId != null && !orderId.equals(existing.orderId()))
                || (sellerId != null && !sellerId.equals(existing.sellerId()))
                || (originalRequestId != null && !originalRequestId.equals(existing.originalRequestId()))) {
            throw DomainException.conflict("IDEMPOTENCY_KEY_REUSED", "同一请求 ID 不能用于不同的资金参数");
        }
        return existing;
    }

    static Optional<PaymentResult> queryPayment(JdbcClient db, String requestId) {
        return queryPayment(db, requestId, false);
    }

    static Optional<PaymentResult> queryPaymentForUpdate(JdbcClient db, String requestId) {
        return queryPayment(db, requestId, true);
    }

    private static Optional<PaymentResult> queryPayment(JdbcClient db, String requestId, boolean forUpdate) {
        return db.sql("select * from payment_request where request_id=:id" + (forUpdate ? " for update" : ""))
                .param("id", requestId)
                .query((rs, row) -> new PaymentResult(rs.getString("request_id"), rs.getString("request_type"),
                        rs.getLong("order_id"), nullableLong(rs, "user_id"), nullableLong(rs, "seller_id"),
                        rs.getBigDecimal("amount"), rs.getString("currency"), rs.getString("status"),
                        rs.getString("transaction_id"), rs.getString("original_request_id"), instant(rs, "completed_at")))
                .optional();
    }

    private static TransactionView mapTransaction(ResultSet rs, int row) throws SQLException {
        return new TransactionView(rs.getString("transaction_id"), nullableLong(rs, "order_id"),
                rs.getLong("user_id"), rs.getString("account_type"), rs.getString("trade_type"),
                rs.getBigDecimal("amount"), rs.getBigDecimal("balance_after"), rs.getString("currency"),
                rs.getString("reversal_of"), rs.getString("remark"), instant(rs, "created_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}

package com.segroup8.order;

import com.segroup8.order.ApiModels.CreateOrderItem;
import java.math.BigDecimal;
import java.util.List;

interface DownstreamGateway {
    Reservation reserve(String reservationId, long buyerUserId, List<CreateOrderItem> items);
    void confirmReservation(String reservationId);
    void releaseReservation(String reservationId);
    Quote quote(String quoteRequestId, long buyerUserId, BigDecimal totalAmount, Long voucherId);
    RemoteResult debit(String paymentRequestId, long buyerUserId, BigDecimal amount, String payMode, String payChannel);
    RemoteResult paymentResult(String paymentRequestId);
    RemoteResult refund(String refundRequestId, long orderId, long buyerUserId, BigDecimal amount);
    RemoteResult refundResult(String refundRequestId);
    RemoteResult settle(String settlementRequestId, long orderId, long sellerUserId, BigDecimal amount);
    RemoteResult settlementResult(String settlementRequestId);
    void releaseVoucher(String releaseRequestId, Long voucherId, long buyerUserId);

    record ProductSnapshot(long productId, String productName, BigDecimal price, int quantity,
            long sellerUserId, Long shopId) {}
    record Reservation(String reservationId, List<ProductSnapshot> items) {}
    record Quote(BigDecimal payableAmount, BigDecimal voucherDiscountAmount,
            BigDecimal sellerBearAmount, BigDecimal platformBearAmount) {}
    enum RemoteResult { SUCCEEDED, FAILED, UNKNOWN }
}

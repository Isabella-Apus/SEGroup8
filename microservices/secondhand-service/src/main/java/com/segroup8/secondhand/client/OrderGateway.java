package com.segroup8.secondhand.client;

import com.segroup8.secondhand.domain.TradeOrderRequest;
import java.util.Optional;

public interface OrderGateway {
    OrderReceipt createSecondhandOrder(TradeOrderRequest request);

    Optional<OrderReceipt> findByBusinessKey(String businessKey);

    record OrderReceipt(long orderId, String orderNo, String status) {
    }
}

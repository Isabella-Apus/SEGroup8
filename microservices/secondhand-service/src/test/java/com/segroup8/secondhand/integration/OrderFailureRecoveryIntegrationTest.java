package com.segroup8.secondhand.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.segroup8.secondhand.client.OrderGateway.OrderReceipt;
import com.segroup8.secondhand.client.OrderServiceUnavailableException;
import com.segroup8.secondhand.service.TradeApplicationService;
import com.segroup8.secondhand.service.TradeOrderCoordinator;
import com.segroup8.secondhand.support.SecondhandIntegrationSupport;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("DOMAIN_D")
@Tag("FAULT_INJECTION")
class OrderFailureRecoveryIntegrationTest extends SecondhandIntegrationSupport {
    @Autowired TradeApplicationService trades;
    @Autowired TradeOrderCoordinator coordinator;

    @Test
    void unavailableOrderServiceReturnsProcessingThenRecoversWithSameBusinessKey() {
        long productId = seedApprovedProduct(10, "故障恢复教材", "66.00", false);
        when(orderGateway.createSecondhandOrder(any()))
                .thenThrow(new OrderServiceUnavailableException("simulated timeout"));
        when(orderGateway.findByBusinessKey(anyString())).thenReturn(Optional.empty());

        var processing = trades.buy(20, productId, 1, "恢复测试");
        assertThat(processing.requestStatus()).isEqualTo("RETRY");
        assertThat(db.queryForObject("select status from secondhand_product where id=?", Integer.class, productId))
                .isEqualTo(4);
        String businessKey = processing.orderBusinessKey();
        long retryDelaySeconds = db.queryForObject(
                "select TIMESTAMPDIFF(SECOND,CURRENT_TIMESTAMP,next_retry_at) "
                        + "from trade_order_request where order_business_key=?",
                Long.class, businessKey);
        assertThat(retryDelaySeconds).isBetween(0L, 2L);

        doReturn(new OrderReceipt(9001, "ORD9001", "PENDING_PAY"))
                .when(orderGateway).createSecondhandOrder(any());
        db.update("update trade_order_request set next_retry_at=CURRENT_TIMESTAMP where order_business_key=?",
                businessKey);
        coordinator.recoverPending(10);

        assertThat(db.queryForObject("select request_status from trade_order_request where order_business_key=?",
                String.class, businessKey)).isEqualTo("CREATED");
        assertThat(db.queryForObject("select order_id from trade_order_request where order_business_key=?",
                Long.class, businessKey)).isEqualTo(9001L);
        assertThat(db.queryForObject("select count(*) from trade_order_request", Integer.class)).isEqualTo(1);
    }

    @Test
    void retryThresholdReleasesFrozenProductWithoutWritingAnyOrderTable() {
        long productId = seedApprovedProduct(10, "重试上限教材", "55.00", false);
        doThrow(new OrderServiceUnavailableException("order offline"))
                .when(orderGateway).createSecondhandOrder(any());
        when(orderGateway.findByBusinessKey(anyString())).thenReturn(Optional.empty());

        var processing = trades.buy(20, productId, 1, null);
        for (int attempt = 0; attempt < 2; attempt++) {
            db.update("update trade_order_request set next_retry_at=CURRENT_TIMESTAMP where order_business_key=?",
                    processing.orderBusinessKey());
            coordinator.recoverPending(10);
        }

        assertThat(db.queryForObject("select request_status from trade_order_request where order_business_key=?",
                String.class, processing.orderBusinessKey())).isEqualTo("FAILED");
        assertThat(db.queryForObject("select status from secondhand_product where id=?", Integer.class, productId))
                .isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from outbox_event where event_type='SecondhandTradeOrderFailed.v1'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void inconclusiveLookupNeverReleasesFrozenProductAtRetryThreshold() {
        long productId = seedApprovedProduct(10, "结果不确定教材", "58.00", false);
        doThrow(new OrderServiceUnavailableException("create response lost"))
                .when(orderGateway).createSecondhandOrder(any());
        doThrow(new OrderServiceUnavailableException("lookup offline"))
                .when(orderGateway).findByBusinessKey(anyString());

        var processing = trades.buy(20, productId, 1, null);
        for (int attempt = 0; attempt < 4; attempt++) {
            db.update("update trade_order_request set next_retry_at=CURRENT_TIMESTAMP where order_business_key=?",
                    processing.orderBusinessKey());
            coordinator.recoverPending(10);
        }

        assertThat(db.queryForObject("select request_status from trade_order_request where order_business_key=?",
                String.class, processing.orderBusinessKey())).isEqualTo("RETRY");
        assertThat(db.queryForObject("select status from secondhand_product where id=?", Integer.class, productId))
                .isEqualTo(4);
        assertThat(db.queryForObject("select count(*) from outbox_event where event_type='SecondhandTradeOrderFailed.v1'",
                Integer.class)).isZero();
    }
}

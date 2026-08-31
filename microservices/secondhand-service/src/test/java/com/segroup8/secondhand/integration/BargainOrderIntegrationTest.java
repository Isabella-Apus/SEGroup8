package com.segroup8.secondhand.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.segroup8.secondhand.api.BargainApplyRequest;
import com.segroup8.secondhand.api.BargainConfirmRequest;
import com.segroup8.secondhand.service.TradeApplicationService;
import com.segroup8.secondhand.support.SecondhandIntegrationSupport;
import java.math.BigDecimal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("DOMAIN_D")
@Tag("UC18")
class BargainOrderIntegrationTest extends SecondhandIntegrationSupport {
    @Autowired TradeApplicationService trades;

    @Test
    void acceptingNegotiationCreatesOnePendingPaymentOrderAndIsIdempotent() {
        long productId = seedApprovedProduct(10, "议价教材", "100.00", true);
        var applied = trades.applyBargain(20,
                new BargainApplyRequest(productId, 10L, new BigDecimal("72.00")));

        var accepted = trades.confirmBargain(10,
                new BargainConfirmRequest(applied.id(), new BigDecimal("75.00"), true));
        var repeated = trades.confirmBargain(10,
                new BargainConfirmRequest(applied.id(), new BigDecimal("75.00"), true));

        assertThat(accepted.status()).isEqualTo("ACCEPTED");
        assertThat(repeated.orderId()).isEqualTo(accepted.orderId());
        assertThat(accepted.orderRequestStatus()).isEqualTo("CREATED");
        assertThat(db.queryForObject("select order_status from trade_order_request where trade_type='BARGAIN'",
                String.class)).isEqualTo("PENDING_PAY");
        assertThat(db.queryForObject("select count(*) from trade_order_request where trade_type='BARGAIN'",
                Integer.class)).isEqualTo(1);
        verify(orderGateway, times(1)).createSecondhandOrder(org.mockito.ArgumentMatchers.any());
    }
}

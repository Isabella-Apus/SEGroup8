package com.segroup8.secondhand.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.segroup8.secondhand.client.OrderGateway;
import com.segroup8.secondhand.client.OrderGateway.OrderReceipt;
import com.segroup8.secondhand.client.IdentityGateway;
import com.segroup8.secondhand.client.IdentityGateway.AddressSnapshot;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class SecondhandIntegrationSupport {
    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate db;
    @MockBean protected OrderGateway orderGateway;
    @MockBean protected IdentityGateway identityGateway;
    private final AtomicLong orderSequence = new AtomicLong(1000);

    @BeforeEach
    protected void resetDatabaseAndOrderGateway() {
        db.update("delete from auction_log");
        db.update("delete from product_auction");
        db.update("delete from product_negotiation");
        db.update("delete from trade_order_request");
        db.update("delete from outbox_event");
        db.update("delete from idempotency_record");
        db.update("delete from secondhand_product");
        when(orderGateway.findByBusinessKey(anyString())).thenReturn(Optional.empty());
        when(identityGateway.resolveAddress(anyLong(), any())).thenAnswer(invocation -> {
            long userId = invocation.getArgument(0);
            Long addressId = invocation.getArgument(1);
            return new AddressSnapshot(addressId == null ? 1L : addressId, userId, "Buyer",
                    "13800008000", "Zhejiang", "Hangzhou", "West Lake Road 1");
        });
        when(orderGateway.createSecondhandOrder(any())).thenAnswer(invocation -> {
            long id = orderSequence.incrementAndGet();
            return new OrderReceipt(id, "ORD" + id, "PENDING_PAY");
        });
    }

    protected long seedApprovedProduct(long sellerId, String name, String price, boolean negotiable) {
        db.update("insert into secondhand_product(seller_user_id,seller_name_snapshot,name,cover,images,description,"
                        + "origin_price,sale_price,category_id,sub_category_id,condition_level,is_negotiable,status,risk_status) "
                        + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                sellerId, "seller-" + sellerId, name, "/images/test.png", "[\"/images/test.png\"]", "可用二手商品",
                new java.math.BigDecimal("200.00"), new java.math.BigDecimal(price), 8, 801, "95%",
                negotiable ? 1 : 0, 1, "APPROVED");
        return db.queryForObject("select max(id) from secondhand_product", Long.class);
    }
}

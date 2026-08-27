package com.segroup8.platform.service.settlement;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.segroup8.platform.entity.Balance;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.entity.TransactionRecord;
import com.segroup8.platform.entity.Voucher;
import com.segroup8.platform.mapper.BalanceMapper;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.TransactionRecordMapper;
import com.segroup8.platform.mapper.VoucherMapper;
import com.segroup8.platform.testsupport.DomainCTestTags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag(DomainCTestTags.DOMAIN_C)
@Tag(DomainCTestTags.PLATFORM)
@Tag("DOMAIN_E")
@Tag("UC23")
class EscrowSettlementServiceTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private ShopMapper shopMapper;
    @Mock
    private SecondhandProductMapper secondhandProductMapper;
    @Mock
    private BalanceMapper balanceMapper;
    @Mock
    private TransactionRecordMapper transactionRecordMapper;
    @Mock
    private VoucherMapper voucherMapper;

    private EscrowSettlementService settlementService;

    @BeforeEach
    void setUp() {
        settlementService = new EscrowSettlementService(
                List.of(new NewProductSettlementStrategy()),
                productMapper,
                shopMapper,
                secondhandProductMapper,
                balanceMapper,
                transactionRecordMapper,
                voucherMapper);

        Product product = new Product();
        product.setId(101L);
        product.setShopId(11L);
        when(productMapper.selectById(101L)).thenReturn(product);

        Shop shop = new Shop();
        shop.setId(11L);
        shop.setOwnerUserId(7L);
        when(shopMapper.selectById(11L)).thenReturn(shop);

        Balance balance = new Balance();
        balance.setUserId(7L);
        balance.setPersonalBalance(BigDecimal.ZERO);
        balance.setBusinessBalance(BigDecimal.ZERO);
        balance.setVersion(0);
        when(balanceMapper.selectById(7L)).thenReturn(balance);
        when(balanceMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(transactionRecordMapper.insert(any(TransactionRecord.class))).thenReturn(1);
    }

    @Test
    void sellerVoucherCreditsSellerWithGrossAmountMinusDiscount() {
        OrderInfo order = orderWithVoucher(new BigDecimal("20.00"), BigDecimal.ZERO);

        Voucher voucher = new Voucher();
        voucher.setId(501L);
        voucher.setIssuerType(1);
        voucher.setVoucherType(1);
        voucher.setShopId(11L);
        when(voucherMapper.selectById(501L)).thenReturn(voucher);

        settlementService.releaseEscrow(order, List.of(item("100.00")));

        assertRecordedSettlementAmount("80.00");
    }

    @Test
    void platformVoucherCreditsSellerWithFullGrossAmount() {
        OrderInfo order = orderWithVoucher(BigDecimal.ZERO, new BigDecimal("20.00"));

        settlementService.releaseEscrow(order, List.of(item("100.00")));

        assertRecordedSettlementAmount("100.00");
        verify(voucherMapper, never()).selectById(any());
    }

    private OrderInfo orderWithVoucher(BigDecimal sellerBearAmount, BigDecimal platformBearAmount) {
        OrderInfo order = new OrderInfo();
        order.setId(9001L);
        order.setVoucherId(501L);
        order.setVoucherDiscountAmount(new BigDecimal("20.00"));
        order.setSellerBearAmount(sellerBearAmount);
        order.setPlatformBearAmount(platformBearAmount);
        return order;
    }

    private OrderItem item(String price) {
        OrderItem item = new OrderItem();
        item.setProductType("NEW");
        item.setProductId(101L);
        item.setPrice(new BigDecimal(price));
        item.setQuantity(1);
        return item;
    }

    private void assertRecordedSettlementAmount(String expected) {
        ArgumentCaptor<TransactionRecord> recordCaptor = ArgumentCaptor.forClass(TransactionRecord.class);
        verify(transactionRecordMapper).insert(recordCaptor.capture());
        assertEquals(new BigDecimal(expected), recordCaptor.getValue().getAmount());
        assertEquals(7L, recordCaptor.getValue().getUserId());
        assertEquals(SettlementAccountType.BUSINESS.name(), recordCaptor.getValue().getAccountType());
    }
}

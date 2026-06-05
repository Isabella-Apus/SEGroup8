package com.segroup8.platform.service;

import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.VoucherSaveRequest;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.entity.UserVoucher;
import com.segroup8.platform.entity.Voucher;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.UserVoucherMapper;
import com.segroup8.platform.mapper.VoucherMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    private VoucherMapper voucherMapper;
    @Mock
    private UserVoucherMapper userVoucherMapper;
    @Mock
    private ShopMapper shopMapper;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void create_shouldUseRealShopIdInsteadOfSellerUserId() {
        UserContext.setUserId(7L);
        Shop shop = new Shop();
        shop.setId(99L);
        shop.setOwnerUserId(7L);
        when(shopMapper.selectOne(any())).thenReturn(shop);

        AtomicReference<Voucher> saved = new AtomicReference<>();
        when(voucherMapper.insert(any(Voucher.class))).thenAnswer(invocation -> {
            Voucher voucher = invocation.getArgument(0);
            voucher.setId(1L);
            saved.set(voucher);
            return 1;
        });
        when(voucherMapper.selectById(anyLong())).thenAnswer(invocation -> saved.get());

        VoucherSaveRequest request = new VoucherSaveRequest();
        request.setName("新人券");
        request.setType(1);
        request.setDiscountAmount(new BigDecimal("10"));
        request.setNoThreshold(true);
        request.setTotalCount(100);
        request.setGrabStartTime(LocalDateTime.now().minusHours(1));
        request.setGrabEndTime(LocalDateTime.now().plusDays(1));
        request.setStartTime(LocalDateTime.now().minusHours(1));
        request.setEndTime(LocalDateTime.now().plusDays(7));

        new VoucherService(voucherMapper, userVoucherMapper, shopMapper).create(request);

        assertEquals(99L, saved.get().getShopId());
        assertEquals(7L, saved.get().getIssuerUserId());
    }

    @Test
    void occupyForOrder_shouldCalculateSellerDiscountFromMatchingShopSubtotal() {
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = new Voucher();
        voucher.setId(5L);
        voucher.setIssuerType(1);
        voucher.setVoucherType(1);
        voucher.setScopeType(1);
        voucher.setShopId(99L);
        voucher.setType(1);
        voucher.setDiscountAmount(new BigDecimal("20"));
        voucher.setMinAmount(new BigDecimal("100"));
        voucher.setStatus(1);
        voucher.setGrabStartTime(now.minusDays(1));
        voucher.setGrabEndTime(now.plusDays(1));
        voucher.setStartTime(now.minusDays(1));
        voucher.setEndTime(now.plusDays(7));
        when(voucherMapper.selectById(5L)).thenReturn(voucher);

        UserVoucher userVoucher = new UserVoucher();
        userVoucher.setId(8L);
        userVoucher.setUserId(3L);
        userVoucher.setVoucherId(5L);
        userVoucher.setStatus(1);
        when(userVoucherMapper.selectOne(any())).thenReturn(userVoucher);
        when(userVoucherMapper.update(any(), any())).thenReturn(1);

        VoucherService.CheckoutDiscount result = new VoucherService(
                voucherMapper, userVoucherMapper, shopMapper).occupyForOrder(
                        5L,
                        3L,
                        10L,
                        Map.of(99L, new BigDecimal("150"), 100L, new BigDecimal("50")),
                        new BigDecimal("200"));

        assertEquals(new BigDecimal("20.00"), result.discountAmount());
        assertEquals(new BigDecimal("20.00"), result.sellerBearAmount());
        assertEquals(new BigDecimal("180.00"), result.payableAmount());
    }
}

package com.segroup8.platform.service;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.entity.UserVoucher;
import com.segroup8.platform.entity.Voucher;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.mapper.UserVoucherMapper;
import com.segroup8.platform.mapper.VoucherMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("DOMAIN_E")
@Tag("UC22")
class UC22VoucherThresholdTest {

    @Mock
    private VoucherMapper voucherMapper;
    @Mock
    private UserVoucherMapper userVoucherMapper;
    @Mock
    private ShopMapper shopMapper;
    @Mock
    private UserMapper userMapper;

    @Test
    void unitUc22001_shopSubtotalBelowThresholdMustNotOccupyVoucher() {
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = new Voucher();
        voucher.setId(22L);
        voucher.setShopId(99L);
        voucher.setScopeType(1);
        voucher.setType(1);
        voucher.setDiscountAmount(new BigDecimal("20.00"));
        voucher.setMinAmount(new BigDecimal("100.00"));
        voucher.setStatus(1);
        voucher.setStartTime(now.minusDays(1));
        voucher.setEndTime(now.plusDays(1));
        voucher.setGrabStartTime(now.minusDays(2));
        voucher.setGrabEndTime(now.plusHours(1));
        when(voucherMapper.selectById(22L)).thenReturn(voucher);

        UserVoucher userVoucher = new UserVoucher();
        userVoucher.setId(220L);
        userVoucher.setUserId(3L);
        userVoucher.setVoucherId(22L);
        userVoucher.setStatus(1);
        when(userVoucherMapper.selectOne(any())).thenReturn(userVoucher);

        VoucherService service = new VoucherService(voucherMapper, userVoucherMapper, shopMapper, userMapper);
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.occupyForOrder(
                        22L,
                        3L,
                        9001L,
                        Map.of(99L, new BigDecimal("80.00"), 100L, new BigDecimal("1000.00")),
                        new BigDecimal("1080.00")));

        assertEquals("当前商品金额未达到优惠券使用门槛", error.getMessage());
        verify(userVoucherMapper, never()).update(any(), any());
    }
}

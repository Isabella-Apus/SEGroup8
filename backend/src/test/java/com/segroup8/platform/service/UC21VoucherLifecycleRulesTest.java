package com.segroup8.platform.service;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.VoucherSaveRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UC21VoucherLifecycleRulesTest {

    @Mock
    private VoucherMapper voucherMapper;
    @Mock
    private UserVoucherMapper userVoucherMapper;
    @Mock
    private ShopMapper shopMapper;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void unitUc21001_discountMustNotExceedThreshold() {
        UserContext.setUserId(7L);
        VoucherSaveRequest request = validRequest();
        request.setDiscountAmount(new BigDecimal("20.00"));
        request.setMinAmount(new BigDecimal("10.00"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> new VoucherService(voucherMapper, userVoucherMapper, shopMapper).create(request));

        assertEquals("优惠金额不能超过门槛金额", error.getMessage());
        verify(voucherMapper, never()).insert(org.mockito.ArgumentMatchers.any(com.segroup8.platform.entity.Voucher.class));
    }

    private VoucherSaveRequest validRequest() {
        LocalDateTime now = LocalDateTime.now();
        VoucherSaveRequest request = new VoucherSaveRequest();
        request.setName("店铺满减券");
        request.setType(1);
        request.setNoThreshold(false);
        request.setDiscountAmount(new BigDecimal("10.00"));
        request.setMinAmount(new BigDecimal("100.00"));
        request.setTotalCount(100);
        request.setGrabStartTime(now.minusHours(1));
        request.setGrabEndTime(now.plusDays(1));
        request.setStartTime(now.minusHours(1));
        request.setEndTime(now.plusDays(7));
        return request;
    }
}

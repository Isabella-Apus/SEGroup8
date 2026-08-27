package com.segroup8.platform.service.impl;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.entity.CreditScoreLog;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.CreditScoreLogMapper;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.ReviewMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.mapper.UserReportMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("DOMAIN_A")
@Tag("UC05")
class CreditServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private CreditScoreLogMapper creditScoreLogMapper;
    @Mock
    private OrderInfoMapper orderInfoMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private UserReportMapper userReportMapper;

    @Test
    void adminAdjust_shouldRejectUnsupportedRole() {
        CreditServiceImpl creditService = new CreditServiceImpl(
                userMapper, creditScoreLogMapper, orderInfoMapper, orderItemMapper, reviewMapper, userReportMapper);
        User user = new User();
        user.setId(3L);
        when(userMapper.selectById(3L)).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> creditService.adminAdjust(3L, "UNKNOWN", -1, "invalid", 1L));

        assertEquals(400, ex.getCode());
    }

    @Test
    void adminAdjust_shouldWriteScoreLog() {
        CreditServiceImpl creditService = new CreditServiceImpl(
                userMapper, creditScoreLogMapper, orderInfoMapper, orderItemMapper, reviewMapper, userReportMapper);
        User user = new User();
        user.setId(3L);
        user.setCreditScore(100);
        when(userMapper.selectById(3L)).thenReturn(user);

        creditService.adminAdjust(3L, "BUYER", -5, "manual adjustment", 1L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(95, userCaptor.getValue().getCreditScore());
        ArgumentCaptor<CreditScoreLog> logCaptor = ArgumentCaptor.forClass(CreditScoreLog.class);
        verify(creditScoreLogMapper).insert(logCaptor.capture());
        assertEquals("ADMIN_ADJUST", logCaptor.getValue().getReasonCode());
        assertEquals(-5, logCaptor.getValue().getDelta());
    }
}

package com.segroup8.platform.service.impl;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.UserBlockRequest;
import com.segroup8.platform.dto.UserReportRequest;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.entity.UserBlock;
import com.segroup8.platform.entity.UserReport;
import com.segroup8.platform.mapper.UserBlockMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.mapper.UserReportMapper;
import com.segroup8.platform.service.CreditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("DOMAIN_A")
@Tag("UC05")
class ReportBlockServiceImplTest {

    @Mock
    private UserReportMapper userReportMapper;
    @Mock
    private UserBlockMapper userBlockMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CreditService creditService;

    private ReportBlockServiceImpl reportBlockService;

    @BeforeEach
    void setUp() {
        reportBlockService = new ReportBlockServiceImpl(userReportMapper, userBlockMapper, userMapper, creditService);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void submitReport_shouldRejectSelfReport() {
        UserContext.setUserId(1L);
        UserReportRequest request = new UserReportRequest();
        request.setReportedId(1L);
        request.setReasonType("FRAUD");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reportBlockService.submitReport(request));

        assertEquals(400, ex.getCode());
        verify(userReportMapper, never()).insert(any(UserReport.class));
    }

    @Test
    void submitReport_shouldInsertPendingReport() {
        UserContext.setUserId(1L);
        User target = new User();
        target.setId(2L);
        when(userMapper.selectById(2L)).thenReturn(target);
        when(userReportMapper.countActiveReport(1L, 2L)).thenReturn(0);

        UserReportRequest request = new UserReportRequest();
        request.setReportedId(2L);
        request.setReasonType("FRAUD");
        request.setReasonDesc("Suspicious trade");
        request.setTradeContext("SH_BUYER");

        reportBlockService.submitReport(request);

        ArgumentCaptor<UserReport> captor = ArgumentCaptor.forClass(UserReport.class);
        verify(userReportMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getReporterId());
        assertEquals(2L, captor.getValue().getReportedId());
        assertEquals("BUYER", captor.getValue().getReporterRole());
        assertEquals("SH_BUYER", captor.getValue().getTradeContext());
        assertEquals(0, captor.getValue().getStatus());
    }

    @Test
    void blockUser_shouldRejectSelfBlock() {
        UserContext.setUserId(1L);
        UserBlockRequest request = new UserBlockRequest();
        request.setTargetUserId(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reportBlockService.blockUser(request));

        assertEquals(400, ex.getCode());
        verify(userBlockMapper, never()).insert(any(UserBlock.class));
    }
}

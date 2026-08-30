package com.segroup8.platform.service.impl;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.MerchantApplicationRejectRequest;
import com.segroup8.platform.entity.MerchantApplication;
import com.segroup8.platform.entity.Notification;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.MerchantApplicationMapper;
import com.segroup8.platform.mapper.NotificationMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.realtime.RealtimePushService;
import com.segroup8.platform.event.ProducerOutboxService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("DOMAIN_A")
@Tag("UC03")
class MerchantApplicationServiceImplTest {

    @Mock
    private MerchantApplicationMapper merchantApplicationMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private ShopMapper shopMapper;

    @Mock
    private RealtimePushService realtimePushService;
    @Mock
    private ProducerOutboxService outbox;

    private MerchantApplicationServiceImpl merchantApplicationService;

    @BeforeEach
    void setUp() {
        merchantApplicationService = new MerchantApplicationServiceImpl(
                merchantApplicationMapper,
                userMapper,
                shopMapper,
                outbox);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void approve_shouldUpgradeRoleAndInsertNotification() {
        UserContext.setUserId(1L);

        User admin = new User();
        admin.setId(1L);
        admin.setRole(RoleEnum.ADMIN.name());

        MerchantApplication app = new MerchantApplication();
        app.setId(8L);
        app.setUserId(3L);
        app.setStatus(0);

        User applicant = new User();
        applicant.setId(3L);
        applicant.setRole(RoleEnum.USER.name());

        when(userMapper.selectById(1L)).thenReturn(admin);
        when(merchantApplicationMapper.selectById(8L)).thenReturn(app);
        when(userMapper.selectById(3L)).thenReturn(applicant);

        merchantApplicationService.approve(8L);

        ArgumentCaptor<MerchantApplication> appCaptor = ArgumentCaptor.forClass(MerchantApplication.class);
        verify(merchantApplicationMapper).updateById(appCaptor.capture());
        assertEquals(1, appCaptor.getValue().getStatus());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(RoleEnum.OFFICIAL_SELLER.name(), userCaptor.getValue().getRole());

        verify(outbox).publish(org.mockito.ArgumentMatchers.eq("MerchantApproved.v1"),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyMap());
    }

    @Test
    void submit_shouldRejectDuplicatePendingApplication() {
        UserContext.setUserId(3L);
        MerchantApplication existing = new MerchantApplication();
        existing.setUserId(3L);
        existing.setStatus(0);
        when(merchantApplicationMapper.selectOne(any())).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> merchantApplicationService.submit(new com.segroup8.platform.dto.MerchantApplicationSubmitRequest()));

        assertEquals(400, ex.getCode());
    }

    @Test
    void reject_shouldPersistReasonAndNotifyApplicant() {
        UserContext.setUserId(1L);
        User admin = new User();
        admin.setId(1L);
        admin.setRole(RoleEnum.ADMIN.name());
        MerchantApplication app = new MerchantApplication();
        app.setId(8L);
        app.setUserId(3L);
        app.setStatus(0);
        when(userMapper.selectById(1L)).thenReturn(admin);
        when(merchantApplicationMapper.selectById(8L)).thenReturn(app);

        MerchantApplicationRejectRequest request = new MerchantApplicationRejectRequest();
        request.setRejectReason("License information is incomplete");

        merchantApplicationService.reject(8L, request);

        ArgumentCaptor<MerchantApplication> appCaptor = ArgumentCaptor.forClass(MerchantApplication.class);
        verify(merchantApplicationMapper).updateById(appCaptor.capture());
        assertEquals(2, appCaptor.getValue().getStatus());
        assertEquals("License information is incomplete", appCaptor.getValue().getRejectReason());
        verify(outbox).notification(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }
}

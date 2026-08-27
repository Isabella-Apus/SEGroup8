package com.segroup8.platform.service.impl;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.entity.Notification;
import com.segroup8.platform.mapper.NotificationMapper;
import com.segroup8.platform.realtime.RealtimePushService;
import com.segroup8.platform.vo.NotificationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("DOMAIN_E")
@Tag("UC25")
class UC25NotificationOwnershipAndPushTest {

    private NotificationMapper notificationMapper;
    private RealtimePushService realtimePushService;
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(NotificationMapper.class);
        realtimePushService = mock(RealtimePushService.class);
        service = new NotificationServiceImpl(notificationMapper, realtimePushService);
    }

    @Test
    void markRead_shouldRejectNotificationOwnedByAnotherUser() {
        Notification notification = new Notification();
        notification.setId(88L);
        notification.setUserId(200L);
        notification.setIsRead(0);
        when(notificationMapper.selectById(88L)).thenReturn(notification);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.markRead(100L, 88L));

        assertEquals(404, error.getCode());
        verify(notificationMapper, never()).update(any(), any());
    }

    @Test
    void createNotification_shouldPersistThenPushOnlyToOwner() {
        when(notificationMapper.insert(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(99L);
            return 1;
        });

        NotificationVO result = service.createNotification(
                100L, " 订单已发货 ", " 请查看物流 ", "/order/1001");

        assertEquals(99L, result.getId());
        assertEquals(100L, captureInserted().getUserId());
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(realtimePushService).pushToUser(eq(100L), eq("NOTIFICATION_CREATED"), payload.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) payload.getValue();
        assertEquals(99L, event.get("id"));
        assertEquals("buyer", event.get("scope"));
    }

    private Notification captureInserted() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        return captor.getValue();
    }
}

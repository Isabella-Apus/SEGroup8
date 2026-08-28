package com.segroup8.platform.controller;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.service.NotificationService;
import com.segroup8.platform.vo.NotificationVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("DOMAIN_E")
@Tag("UC25")
@ExtendWith(MockitoExtension.class)
class NotificationControllerUc25WebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        UserContext.setUserId(25L);
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void list_shouldUseCurrentUserAndScope() throws Exception {
        NotificationVO notification = new NotificationVO();
        notification.setId(25001L);
        notification.setScope("buyer");
        notification.setIsRead(0);
        when(notificationService.listMyNotifications(25L, "buyer")).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications").param("scope", "buyer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(25001L))
                .andExpect(jsonPath("$.data[0].scope").value("buyer"));

        verify(notificationService).listMyNotifications(25L, "buyer");
    }

    @Test
    void markRead_shouldUseCurrentUserAndNotificationId() throws Exception {
        mockMvc.perform(post("/api/notifications/25001/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(notificationService).markRead(25L, 25001L);
    }

    @Test
    void markAllRead_shouldPassOptionalScope() throws Exception {
        mockMvc.perform(post("/api/notifications/read-all").param("scope", "seller"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(notificationService).markAllRead(25L, "seller");
    }

    @Test
    void foreignNotification_shouldKeepUnifiedNotFoundResponse() throws Exception {
        doThrow(new BusinessException(404, "通知不存在"))
                .when(notificationService).markRead(25L, 99999L);

        mockMvc.perform(post("/api/notifications/99999/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("通知不存在"));
    }
}

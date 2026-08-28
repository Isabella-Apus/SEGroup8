package com.segroup8.platform.integration;

import com.segroup8.platform.entity.Notification;
import com.segroup8.platform.mapper.NotificationMapper;
import com.segroup8.platform.realtime.RealtimePushService;
import com.segroup8.platform.service.NotificationService;
import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "/integration/uc25-notification-setup.sql")
@Tag("DOMAIN_E")
@Tag("UC25")
class NotificationFlowUc25IntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationMapper notificationMapper;

    @MockBean
    private RealtimePushService realtimePushService;

    @Test
    void list_shouldIsolateCurrentUserAndApplyBuyerOrSellerScope() throws Exception {
        String token = buyerToken();

        mvc.perform(get("/api/notifications").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[*].id", containsInAnyOrder(25001, 25002, 25003)));

        mvc.perform(get("/api/notifications").param("scope", "buyer")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].scope", containsInAnyOrder("buyer", "buyer")));

        mvc.perform(get("/api/notifications").param("scope", "seller")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(25002));
    }

    @Test
    void markRead_shouldUpdateOwnedNotificationAndHideForeignOrMissingOnes() throws Exception {
        String token = buyerToken();

        mvc.perform(post("/api/notifications/25001/read").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(post("/api/notifications/25004/read").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
        mvc.perform(post("/api/notifications/25999/read").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));

        org.junit.jupiter.api.Assertions.assertEquals(1, notificationMapper.selectById(25001L).getIsRead());
        org.junit.jupiter.api.Assertions.assertEquals(0, notificationMapper.selectById(25004L).getIsRead());
    }

    @Test
    void markAllRead_shouldSupportScopedAndUnscopedUpdates() throws Exception {
        String token = buyerToken();

        mvc.perform(post("/api/notifications/read-all").param("scope", "buyer")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(1, notificationMapper.selectById(25001L).getIsRead());
        org.junit.jupiter.api.Assertions.assertEquals(0, notificationMapper.selectById(25002L).getIsRead());

        mvc.perform(post("/api/notifications/read-all").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(1, notificationMapper.selectById(25002L).getIsRead());
        org.junit.jupiter.api.Assertions.assertEquals(0, notificationMapper.selectById(25004L).getIsRead());
    }

    @Test
    void create_shouldPushOnlyOwnerAndKeepRecordWhenPushFails() {
        notificationService.createNotification(1L, "实时通知", "第一次推送", "/order/1");
        verify(realtimePushService).pushToUser(eq(1L), eq("NOTIFICATION_CREATED"), any(Map.class));

        doThrow(new IllegalStateException("socket unavailable"))
                .when(realtimePushService).pushToUser(eq(1L), eq("NOTIFICATION_CREATED"), any());
        long before = notificationMapper.selectCount(null);

        notificationService.createNotification(1L, "补偿通知", "断线期间仍需持久化", "/order/2");

        org.junit.jupiter.api.Assertions.assertEquals(before + 1, notificationMapper.selectCount(null));
        Notification latest = notificationMapper.selectList(null).stream()
                .filter(item -> "补偿通知".equals(item.getTitle()))
                .findFirst()
                .orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(1L, latest.getUserId());
    }

    private String buyerToken() {
        return jwtUtils.createToken(1L, "buyer1", "USER");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

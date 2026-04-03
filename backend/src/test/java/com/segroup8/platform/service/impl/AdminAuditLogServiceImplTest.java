package com.segroup8.platform.service.impl;

import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.entity.AdminAuditLog;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.AdminAuditLogMapper;
import com.segroup8.platform.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogServiceImplTest {

    @Mock
    private AdminAuditLogMapper adminAuditLogMapper;

    @Mock
    private UserMapper userMapper;

    private AdminAuditLogServiceImpl adminAuditLogService;

    @BeforeEach
    void setUp() {
        adminAuditLogService = new AdminAuditLogServiceImpl(adminAuditLogMapper, userMapper);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void record_shouldInsertAuditLog() {
        UserContext.setUserId(1L);
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        when(userMapper.selectById(1L)).thenReturn(admin);

        adminAuditLogService.record("BAN_USER", "USER", 3L, "测试封禁");

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogMapper).insert(captor.capture());
        AdminAuditLog log = captor.getValue();
        assertEquals(1L, log.getAdminUserId());
        assertEquals("admin", log.getAdminUsername());
        assertEquals("BAN_USER", log.getAction());
        assertEquals("USER", log.getTargetType());
        assertEquals(3L, log.getTargetId());
    }
}
